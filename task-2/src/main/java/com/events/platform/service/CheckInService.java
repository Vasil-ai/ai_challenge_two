package com.events.platform.service;

import com.events.platform.domain.CheckIn;
import com.events.platform.domain.Event;
import com.events.platform.domain.RsvpStatus;
import com.events.platform.domain.Ticket;
import com.events.platform.domain.User;
import com.events.platform.repo.CheckInRepository;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.RsvpRegistrationRepository;
import com.events.platform.repo.TicketRepository;
import com.events.platform.web.dto.CheckInStatsResponse;
import com.events.platform.web.error.ApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final CheckInRepository checkInRepository;
    private final RsvpRegistrationRepository rsvpRegistrationRepository;
    private final AccessControlService accessControlService;

    public CheckInStatsResponse stats(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        return statsInternal(event);
    }

    public CheckInStatsResponse statsForEvent(Event event) {
        return statsInternal(event);
    }

    private CheckInStatsResponse statsInternal(Event event) {
        long going =
                rsvpRegistrationRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.CONFIRMED);
        long waitlist =
                rsvpRegistrationRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.WAITLISTED);
        long checkedIn = checkInRepository.countByEventIdAndUndoneAtIsNull(event.getId());
        return new CheckInStatsResponse(going, waitlist, checkedIn);
    }

    @Transactional
    public CheckInStatsResponse checkIn(Long eventId, String rawCode, String sessionId, User checker) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "X-CheckIn-Session header required");
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        if (!accessControlService.canCheckIn(event, checker)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        String code = rawCode.trim();
        Ticket ticket =
                ticketRepository
                        .findByPublicCodeIgnoreCase(code)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid code"));
        if (!ticket.getRegistration().getEvent().getId().equals(eventId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Invalid code");
        }
        if (checkInRepository.existsByTicketAndUndoneAtIsNull(ticket)) {
            throw new ApiException(HttpStatus.CONFLICT, "Already checked in");
        }
        CheckIn c = new CheckIn();
        c.setTicket(ticket);
        c.setEvent(event);
        c.setCheckedInAt(Instant.now());
        c.setCheckedInBy(checker);
        c.setSessionId(sessionId);
        checkInRepository.save(c);
        return statsInternal(event);
    }

    @Transactional
    public CheckInStatsResponse undoLast(Long eventId, String sessionId, User checker) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "X-CheckIn-Session header required");
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        if (!accessControlService.canCheckIn(event, checker)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        CheckIn last =
                checkInRepository
                        .findTopByEventIdAndSessionIdAndUndoneAtIsNullOrderByCheckedInAtDesc(eventId, sessionId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nothing to undo"));
        last.setUndoneAt(Instant.now());
        checkInRepository.save(last);
        return statsInternal(event);
    }

    public CheckInStatsResponse statsForChecker(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        if (!accessControlService.canCheckIn(event, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return statsInternal(event);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
