package com.events.platform.service;

import com.events.platform.domain.Event;
import com.events.platform.domain.EventFeedback;
import com.events.platform.domain.RsvpStatus;
import com.events.platform.domain.User;
import com.events.platform.repo.EventFeedbackRepository;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.RsvpRegistrationRepository;
import com.events.platform.web.error.ApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final EventRepository eventRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final RsvpRegistrationRepository rsvpRegistrationRepository;

    @Transactional
    public EventFeedback submit(Long eventId, User user, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stars must be 1-5");
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        Instant now = Instant.now();
        if (!event.isEnded(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feedback allowed after event ends");
        }
        rsvpRegistrationRepository
                .findByEventIdAndUserIdAndStatus(eventId, user.getId(), RsvpStatus.CONFIRMED)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Confirmed RSVP required"));
        if (eventFeedbackRepository.findByEventIdAndUserId(eventId, user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Feedback already submitted");
        }
        EventFeedback fb = new EventFeedback();
        fb.setEvent(event);
        fb.setUser(user);
        fb.setStars(stars);
        fb.setComment(comment);
        return eventFeedbackRepository.save(fb);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
