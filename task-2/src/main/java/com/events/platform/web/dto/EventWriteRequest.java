package com.events.platform.web.dto;

import com.events.platform.domain.EventVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EventWriteRequest(
        @NotBlank String title,
        String description,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        String timezone,
        String venueText,
        String onlineUrl,
        @Min(1) int capacity,
        String coverImageUrl,
        EventVisibility visibility) {}
