package com.events.platform.web;

import com.events.platform.domain.Event;
import com.events.platform.web.dto.EventResponse;
import java.time.Instant;

public final class EventMapper {

    private EventMapper() {}

    public static EventResponse toResponse(Event e, Instant now) {
        return new EventResponse(
                e.getId(),
                e.getHost().getId(),
                e.getHost().getSlug(),
                e.getSlug(),
                e.getTitle(),
                e.getDescription(),
                e.getStartAt(),
                e.getEndAt(),
                e.getTimezone(),
                e.getVenueText(),
                e.getOnlineUrl(),
                e.getCapacity(),
                e.getCoverImageUrl(),
                e.getVisibility(),
                e.getLifecycle(),
                e.isEnded(now),
                e.isModerationHidden());
    }
}
