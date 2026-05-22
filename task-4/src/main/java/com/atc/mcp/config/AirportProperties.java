package com.atc.mcp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;

/**
 * Type-safe airport configuration loaded from environment variables (prefix {@code atc}).
 * All durations are expressed in seconds. Values are validated by Bean Validation
 * at bind time and again by {@link AirportPropertiesValidator} for cross-field rules.
 */
@Validated
@ConfigurationProperties(prefix = "atc")
public record AirportProperties(

        @NotEmpty
        List<@Valid RunwaySpec> runways,

        @Positive
        int gateCount,

        @Positive
        int groundCrewCount,

        @NotNull
        @Valid
        Separation separation,

        @PositiveOrZero
        long gateTurnaroundSeconds,

        @PositiveOrZero
        long dependencyBufferSeconds,

        @Positive
        long schedulingHorizonSeconds,

        @NotNull
        @Valid
        OperationDuration operationDuration,

        @NotNull
        Instant epoch
) {

    /**
     * Single runway capability descriptor parsed from {@code id:lengthMeters}.
     */
    public record RunwaySpec(
            @NotNull String id,
            @Positive int lengthMeters
    ) { }

    /**
     * Runway separation buffers split by previous-vs-next operation type.
     */
    public record Separation(
            @PositiveOrZero long takeoffSeconds,
            @PositiveOrZero long landingSeconds,
            @PositiveOrZero long mixedSeconds
    ) {
        public long forTransition(boolean prevWasArrival, boolean nextIsArrival) {
            if (prevWasArrival && nextIsArrival) {
                return landingSeconds;
            }
            if (!prevWasArrival && !nextIsArrival) {
                return takeoffSeconds;
            }
            return mixedSeconds;
        }
    }

    /**
     * Default operation durations used when a flight does not provide its own.
     */
    public record OperationDuration(
            @Positive long arrivalSeconds,
            @Positive long departureSeconds
    ) { }
}
