package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.repo.HostMembershipRepository;
import com.events.platform.repo.TicketRepository;
import com.events.platform.service.EventService;
import com.events.platform.service.RsvpService;
import com.events.platform.web.dto.EventResponse;
import com.events.platform.web.dto.TicketRowResponse;
import com.events.platform.web.dto.UserResponse;
import com.events.platform.web.error.ApiException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeApiController {

    private final RsvpService rsvpService;
    private final TicketRepository ticketRepository;
    private final EventService eventService;
    private final HostMembershipRepository hostMembershipRepository;

    @GetMapping
    public UserResponse me() {
        User u = SecurityUtils.requireUser();
        return UserResponseMapper.from(u, hostMembershipRepository);
    }

    @GetMapping("/tickets")
    public List<TicketRowResponse> tickets() {
        User user = SecurityUtils.requireUser();
        Instant now = Instant.now();
        var registrations = rsvpService.findConfirmedUpcoming(user.getId(), now);
        List<TicketRowResponse> rows =
                registrations.stream()
                        .map(
                                reg -> {
                                    var ticket =
                                            ticketRepository
                                                    .findByRegistrationId(reg.getId())
                                                    .orElseThrow(
                                                            () ->
                                                                    new ApiException(
                                                                            HttpStatus
                                                                                    .INTERNAL_SERVER_ERROR,
                                                                            "Ticket missing"));
                                    return new TicketRowResponse(
                                            EventMapper.toResponse(reg.getEvent(), now),
                                            ticket.getPublicCode(),
                                            reg.isPromotionPending());
                                })
                        .toList();
        rsvpService.clearPromotionFlags(user);
        return rows;
    }

    @GetMapping("/events")
    public List<EventResponse> myEvents(
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String q) {
        User user = SecurityUtils.requireUser();
        return eventService.myEvents(user.getId(), hostId, from, to, q).stream()
                .map(e -> EventMapper.toResponse(e, Instant.now()))
                .toList();
    }
}
