package com.events.platform.web.dto;

import com.events.platform.domain.EventLifecycle;
import com.events.platform.domain.EventVisibility;
import java.time.Instant;

public record EventResponse(
        Long id,
        Long hostId,
        String hostSlug,
        String slug,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        String timezone,
        String venueText,
        String onlineUrl,
        int capacity,
        String coverImageUrl,
        EventVisibility visibility,
        EventLifecycle lifecycle,
        boolean ended,
        boolean moderationHidden) {}
