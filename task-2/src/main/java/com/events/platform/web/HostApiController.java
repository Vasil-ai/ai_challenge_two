package com.events.platform.web;

import com.events.platform.domain.Event;
import com.events.platform.domain.EventLifecycle;
import com.events.platform.domain.Host;
import com.events.platform.domain.HostMembership;
import com.events.platform.domain.MembershipRole;
import com.events.platform.domain.User;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.HostMembershipRepository;
import com.events.platform.repo.HostRepository;
import com.events.platform.service.EventService;
import com.events.platform.service.SlugService;
import com.events.platform.web.dto.BecomeHostRequest;
import com.events.platform.web.dto.EventWriteRequest;
import com.events.platform.web.dto.EventResponse;
import com.events.platform.web.dto.HostPublicResponse;
import com.events.platform.web.dto.HostResponse;
import com.events.platform.web.error.ApiException;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hosts")
@RequiredArgsConstructor
public class HostApiController {

    private final HostRepository hostRepository;
    private final HostMembershipRepository hostMembershipRepository;
    private final EventRepository eventRepository;
    private final SlugService slugService;
    private final EventService eventService;

    @PostMapping
    public HostResponse becomeHost(@Valid @RequestBody BecomeHostRequest req) {
        User user = SecurityUtils.requireUser();
        String slug = slugService.slugify(req.slug(), "host");
        if (hostRepository.existsBySlug(slug)) {
            throw new ApiException(HttpStatus.CONFLICT, "Slug already taken");
        }
        Host h = new Host();
        h.setSlug(slug);
        h.setDisplayName(req.displayName().trim());
        h.setBio(req.bio());
        h.setContactEmail(req.contactEmail().trim().toLowerCase());
        h.setLogoUrl(req.logoUrl());
        hostRepository.save(h);

        HostMembership m = new HostMembership();
        m.setUserId(user.getId());
        m.setHostId(h.getId());
        m.setRole(MembershipRole.HOST);
        hostMembershipRepository.save(m);

        return toHostResponse(h);
    }

    @PatchMapping("/{hostId}")
    public HostResponse patch(@PathVariable Long hostId, @Valid @RequestBody BecomeHostRequest req) {
        User user = SecurityUtils.requireUser();
        if (!hostMembershipRepository.existsByHostIdAndUserIdAndRole(
                hostId, user.getId(), MembershipRole.HOST)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        Host h = hostRepository.findById(hostId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        String newSlug = slugService.slugify(req.slug(), "host");
        if (!newSlug.equals(h.getSlug()) && hostRepository.existsBySlug(newSlug)) {
            throw new ApiException(HttpStatus.CONFLICT, "Slug already taken");
        }
        h.setSlug(newSlug);
        h.setDisplayName(req.displayName().trim());
        h.setBio(req.bio());
        h.setContactEmail(req.contactEmail().trim().toLowerCase());
        h.setLogoUrl(req.logoUrl());
        hostRepository.save(h);
        return toHostResponse(h);
    }

    @GetMapping("/by-slug/{slug}")
    public HostPublicResponse publicProfile(@PathVariable String slug) {
        Host h =
                hostRepository
                        .findBySlug(slug)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        Instant now = Instant.now();
        List<EventResponse> events =
                eventRepository.findByHostIdAndLifecycleOrderByStartAtDesc(h.getId(), EventLifecycle.PUBLISHED)
                        .stream()
                        .filter(e -> !e.isModerationHidden())
                        .map(e -> EventMapper.toResponse(e, now))
                        .toList();
        return new HostPublicResponse(toHostResponse(h), events);
    }

    @PostMapping("/{hostId}/events")
    public EventResponse createEvent(@PathVariable Long hostId, @Valid @RequestBody EventWriteRequest req) {
        User user = SecurityUtils.requireUser();
        Event created = eventService.create(hostId, user, req);
        return EventMapper.toResponse(created, Instant.now());
    }

    private static HostResponse toHostResponse(Host h) {
        return new HostResponse(h.getId(), h.getSlug(), h.getDisplayName(), h.getLogoUrl(), h.getBio(), h.getContactEmail());
    }
}
