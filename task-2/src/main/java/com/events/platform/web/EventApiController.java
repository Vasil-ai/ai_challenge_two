package com.events.platform.web;

import com.events.platform.domain.Event;
import com.events.platform.domain.User;
import com.events.platform.repo.TicketRepository;
import com.events.platform.service.EventService;
import com.events.platform.service.RsvpService;
import com.events.platform.web.dto.EventResponse;
import com.events.platform.web.dto.EventWriteRequest;
import com.events.platform.web.dto.RsvpResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventApiController {

    private final EventService eventService;
    private final RsvpService rsvpService;
    private final TicketRepository ticketRepository;

    @GetMapping
    public List<EventResponse> explore(
            @RequestParam(defaultValue = "false") boolean includePast,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location) {
        Instant now = Instant.now();
        return eventService.explore(includePast, from, to, query, location).stream()
                .map(e -> EventMapper.toResponse(e, now))
                .toList();
    }

    @GetMapping("/{id}")
    public EventResponse getOne(@PathVariable Long id) {
        User user = SecurityUtils.currentUser();
        Instant now = Instant.now();
        Event e = eventService.getPublishedOrAuthorized(id, user);
        return EventMapper.toResponse(e, now);
    }

    @PostMapping("/{id}/rsvp")
    @ResponseStatus(HttpStatus.CREATED)
    public RsvpResponse rsvp(@PathVariable Long id) {
        User user = SecurityUtils.requireUser();
        var reg = rsvpService.rsvp(id, user);
        Instant now = Instant.now();
        return switch (reg.getStatus()) {
            case CONFIRMED -> ticketRepository
                    .findByRegistrationId(reg.getId())
                    .map(
                            t ->
                                    new RsvpResponse(
                                            "CONFIRMED",
                                            t.getPublicCode(),
                                            null,
                                            reg.isPromotionPending(),
                                            EventMapper.toResponse(reg.getEvent(), now)))
                    .orElseThrow();
            case WAITLISTED ->
                    new RsvpResponse(
                            "WAITLISTED",
                            null,
                            reg.getWaitlistPosition(),
                            reg.isPromotionPending(),
                            EventMapper.toResponse(reg.getEvent(), now));
            default ->
                    new RsvpResponse(
                            reg.getStatus().name(),
                            null,
                            reg.getWaitlistPosition(),
                            reg.isPromotionPending(),
                            EventMapper.toResponse(reg.getEvent(), now));
        };
    }

    @DeleteMapping("/{id}/rsvp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRsvp(@PathVariable Long id) {
        User user = SecurityUtils.requireUser();
        rsvpService.cancel(id, user);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventWriteRequest req) {
        User user = SecurityUtils.requireUser();
        Event e = eventService.update(id, user, req);
        return EventMapper.toResponse(e, Instant.now());
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id) {
        User user = SecurityUtils.requireUser();
        Event e = eventService.publish(id, user);
        return EventMapper.toResponse(e, Instant.now());
    }

    @PostMapping("/{id}/unpublish")
    public EventResponse unpublish(@PathVariable Long id) {
        User user = SecurityUtils.requireUser();
        Event e = eventService.unpublish(id, user);
        return EventMapper.toResponse(e, Instant.now());
    }

    @PostMapping("/{id}/duplicate")
    public EventResponse duplicate(@PathVariable Long id) {
        User user = SecurityUtils.requireUser();
        Event e = eventService.duplicate(id, user);
        return EventMapper.toResponse(e, Instant.now());
    }

    @PatchMapping("/{id}/capacity")
    public EventResponse capacity(@PathVariable Long id, @RequestParam int value) {
        User user = SecurityUtils.requireUser();
        Event e = eventService.patchCapacity(id, user, value);
        return EventMapper.toResponse(e, Instant.now());
    }
}
