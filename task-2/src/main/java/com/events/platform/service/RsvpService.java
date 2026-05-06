package com.events.platform.service;

import com.events.platform.domain.Event;
import com.events.platform.domain.EventLifecycle;
import com.events.platform.domain.RsvpRegistration;
import com.events.platform.domain.RsvpStatus;
import com.events.platform.domain.Ticket;
import com.events.platform.domain.User;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.RsvpRegistrationRepository;
import com.events.platform.repo.TicketRepository;
import com.events.platform.web.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final EventRepository eventRepository;
    private final RsvpRegistrationRepository rsvpRegistrationRepository;
    private final TicketRepository ticketRepository;

    private static String newTicketCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @Transactional
    public RsvpRegistration rsvp(Long eventId, User user) {
        Instant now = Instant.now();
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> notFound());
        validateRsvpAllowed(event, now);

        Optional<RsvpRegistration> active =
                rsvpRegistrationRepository.findActiveForUserAndEvent(user.getId(), eventId);
        if (active.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Already RSVPed for this event");
        }

        long confirmedCount = rsvpRegistrationRepository.countByEventIdAndStatus(eventId, RsvpStatus.CONFIRMED);
        RsvpRegistration reg = new RsvpRegistration();
        reg.setEvent(event);
        reg.setUser(user);

        if (confirmedCount < event.getCapacity()) {
            reg.setStatus(RsvpStatus.CONFIRMED);
            reg.setWaitlistPosition(null);
            reg = rsvpRegistrationRepository.save(reg);
            issueTicket(reg);
        } else {
            int pos = rsvpRegistrationRepository.maxWaitlistPosition(eventId) + 1;
            reg.setStatus(RsvpStatus.WAITLISTED);
            reg.setWaitlistPosition(pos);
            rsvpRegistrationRepository.save(reg);
        }
        return rsvpRegistrationRepository
                .findByIdWithEventAndHost(reg.getId())
                .orElseThrow();
    }

    private void validateRsvpAllowed(Event event, Instant now) {
        if (event.getLifecycle() != EventLifecycle.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not published");
        }
        if (event.isEnded(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event has ended");
        }
        if (event.isModerationHidden()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        }
    }

    @Transactional
    public void cancel(Long eventId, User user) {
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> notFound());
        RsvpRegistration reg =
                rsvpRegistrationRepository
                        .findActiveForUserAndEvent(user.getId(), eventId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RSVP not found"));

        boolean wasConfirmed = reg.getStatus() == RsvpStatus.CONFIRMED;
        if (wasConfirmed) {
            ticketRepository.deleteByRegistrationId(reg.getId());
        }

        reg.setStatus(RsvpStatus.CANCELLED);
        reg.setWaitlistPosition(null);
        reg.setUpdatedAt(Instant.now());
        rsvpRegistrationRepository.save(reg);

        if (wasConfirmed) {
            promoteNextWaitlisted(event);
        }
    }

    /**
     * Call inside transaction with event row locked when capacity grows.
     */
    public void promoteWaitlistWhileHasSeats(Event event) {
        while (true) {
            long confirmed = rsvpRegistrationRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.CONFIRMED);
            if (confirmed >= event.getCapacity()) {
                break;
            }
            if (!promoteNextWaitlisted(event)) {
                break;
            }
        }
    }

    private boolean promoteNextWaitlisted(Event event) {
        var waitlist =
                rsvpRegistrationRepository.findWaitlistOrdered(event.getId(), RsvpStatus.WAITLISTED);
        if (waitlist.isEmpty()) {
            return false;
        }
        RsvpRegistration head = waitlist.get(0);
        head.setStatus(RsvpStatus.CONFIRMED);
        head.setWaitlistPosition(null);
        head.setPromotedAt(Instant.now());
        head.setPromotionPending(true);
        head.setUpdatedAt(Instant.now());
        rsvpRegistrationRepository.save(head);
        issueTicket(head);
        return true;
    }

    private void issueTicket(RsvpRegistration reg) {
        Ticket ticket = new Ticket();
        ticket.setRegistration(reg);
        ticket.setPublicCode(newTicketCode());
        ticketRepository.save(ticket);
    }

    @Transactional
    public void clearPromotionFlags(User user) {
        var upcoming =
                rsvpRegistrationRepository.findConfirmedUpcomingForUser(user.getId(), Instant.now());
        for (RsvpRegistration r : upcoming) {
            if (r.isPromotionPending()) {
                r.setPromotionPending(false);
                r.setUpdatedAt(Instant.now());
                rsvpRegistrationRepository.save(r);
            }
        }
    }

    public List<RsvpRegistration> findConfirmedUpcoming(Long userId, Instant now) {
        return rsvpRegistrationRepository.findConfirmedUpcomingForUser(userId, now);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Event not found");
    }
}
