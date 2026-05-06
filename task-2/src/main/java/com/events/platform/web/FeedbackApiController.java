package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.FeedbackService;
import com.events.platform.web.dto.FeedbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedbackApiController {

    private final FeedbackService feedbackService;

    @PostMapping("/api/events/{eventId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public void feedback(@PathVariable Long eventId, @Valid @RequestBody FeedbackRequest req) {
        User user = SecurityUtils.requireUser();
        feedbackService.submit(eventId, user, req.stars(), req.comment());
    }
}
