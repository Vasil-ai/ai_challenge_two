package com.events.platform.service;

import com.events.platform.domain.Event;
import com.events.platform.domain.EventLifecycle;
import com.events.platform.domain.EventVisibility;
import com.events.platform.domain.Host;
import com.events.platform.domain.User;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.HostRepository;
import com.events.platform.web.dto.EventWriteRequest;
import com.events.platform.web.error.ApiException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final HostRepository hostRepository;
    private final SlugService slugService;
    private final AccessControlService accessControlService;
    private final RsvpService rsvpService;

    public Event getPublishedOrAuthorized(Long id, User user) {
        Event event =
                eventRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        if (event.isModerationHidden()) {
            boolean allowed =
                    user != null && accessControlService.canManageHost(event.getHost().getId(), user);
            if (!allowed) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
            }
        }
        if (event.getLifecycle() != EventLifecycle.PUBLISHED) {
            if (user == null || !accessControlService.canManageHost(event.getHost().getId(), user)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
            }
        }
        return event;
    }

    public List<Event> explore(
            boolean includePast,
            Instant from,
            Instant to,
            String query,
            String location) {
        String q = blankToNull(query);
        String loc = blankToNull(location);
        return eventRepository.searchExplore(
                EventLifecycle.PUBLISHED,
                EventVisibility.PUBLIC,
                includePast,
                Instant.now(),
                from,
                to,
                q,
                loc);
    }

    public List<Event> listForHostDashboard(Long hostId, User user) {
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return eventRepository.findByHostIdOrderByStartAtDesc(hostId);
    }

    public List<Event> myEvents(Long userId, Long hostId, Instant from, Instant to, String q) {
        return eventRepository.findEventsForMember(
                userId, hostId, from, to, blankToNull(q));
    }

    @Transactional
    public Event create(Long hostId, User user, EventWriteRequest req) {
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        Host host = hostRepository.findById(hostId).orElseThrow(() -> notFound());
        Event e = mapNew(host, req);
        return eventRepository.save(e);
    }

    @Transactional
    public Event update(Long eventId, User user, EventWriteRequest req) {
        Event e = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> notFound());
        requireHost(e.getHost().getId(), user);
        apply(e, req);
        return eventRepository.save(e);
    }

    @Transactional
    public Event publish(Long eventId, User user) {
        Event e = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> notFound());
        requireHost(e.getHost().getId(), user);
        e.setLifecycle(EventLifecycle.PUBLISHED);
        return eventRepository.save(e);
    }

    @Transactional
    public Event unpublish(Long eventId, User user) {
        Event e = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> notFound());
        requireHost(e.getHost().getId(), user);
        e.setLifecycle(EventLifecycle.DRAFT);
        return eventRepository.save(e);
    }

    @Transactional
    public Event duplicate(Long eventId, User user) {
        Event src = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        requireHost(src.getHost().getId(), user);
        Event copy = new Event();
        copy.setHost(src.getHost());
        copy.setSlug(uniqueSlug(src.getHost().getId(), src.getSlug() + "-copy"));
        copy.setTitle(src.getTitle() + " (copy)");
        copy.setDescription(src.getDescription());
        copy.setStartAt(src.getStartAt());
        copy.setEndAt(src.getEndAt());
        copy.setTimezone(src.getTimezone());
        copy.setVenueText(src.getVenueText());
        copy.setOnlineUrl(src.getOnlineUrl());
        copy.setCapacity(src.getCapacity());
        copy.setCoverImageUrl(src.getCoverImageUrl());
        copy.setVisibility(src.getVisibility());
        copy.setLifecycle(EventLifecycle.DRAFT);
        return eventRepository.save(copy);
    }

    @Transactional
    public Event patchCapacity(Long eventId, User user, int newCapacity) {
        if (newCapacity < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid capacity");
        }
        Event e = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> notFound());
        requireHost(e.getHost().getId(), user);
        int old = e.getCapacity();
        e.setCapacity(newCapacity);
        eventRepository.save(e);
        if (newCapacity > old) {
            rsvpService.promoteWaitlistWhileHasSeats(e);
        }
        return eventRepository.findById(eventId).orElseThrow();
    }

    private Event mapNew(Host host, EventWriteRequest req) {
        Event e = new Event();
        e.setHost(host);
        e.setSlug(uniqueSlug(host.getId(), slugService.slugify(req.title(), "event")));
        apply(e, req);
        e.setLifecycle(EventLifecycle.DRAFT);
        return e;
    }

    private void apply(Event e, EventWriteRequest req) {
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setStartAt(req.startAt());
        e.setEndAt(req.endAt());
        e.setTimezone(req.timezone() != null ? req.timezone() : "UTC");
        e.setVenueText(req.venueText());
        e.setOnlineUrl(req.onlineUrl());
        e.setCapacity(req.capacity());
        e.setCoverImageUrl(req.coverImageUrl());
        e.setVisibility(req.visibility() != null ? req.visibility() : EventVisibility.PUBLIC);
        if (!req.endAt().isAfter(req.startAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        }
    }

    private String uniqueSlug(Long hostId, String base) {
        String candidate = base;
        int i = 0;
        while (eventRepository.findByHostIdAndSlug(hostId, candidate).isPresent()) {
            i++;
            candidate = base + "-" + i;
        }
        return candidate.substring(0, Math.min(candidate.length(), 160));
    }

    private void requireHost(Long hostId, User user) {
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
