package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.CheckInService;
import com.events.platform.web.dto.CheckInBody;
import com.events.platform.web.dto.CheckInStatsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/check-in")
@RequiredArgsConstructor
public class CheckInApiController {

    private final CheckInService checkInService;

    @GetMapping("/stats")
    public CheckInStatsResponse stats(@org.springframework.web.bind.annotation.PathVariable Long eventId) {
        User user = SecurityUtils.requireUser();
        return checkInService.statsForChecker(eventId, user);
    }

    @PostMapping
    public CheckInStatsResponse checkIn(
            @org.springframework.web.bind.annotation.PathVariable Long eventId,
            @RequestHeader(value = "X-CheckIn-Session", required = false) String sessionId,
            @Valid @RequestBody CheckInBody body) {
        User user = SecurityUtils.requireUser();
        return checkInService.checkIn(eventId, body.code(), sessionId, user);
    }

    @PostMapping("/undo-last")
    public CheckInStatsResponse undo(
            @org.springframework.web.bind.annotation.PathVariable Long eventId,
            @RequestHeader(value = "X-CheckIn-Session", required = false) String sessionId) {
        User user = SecurityUtils.requireUser();
        return checkInService.undoLast(eventId, sessionId, user);
    }
}
