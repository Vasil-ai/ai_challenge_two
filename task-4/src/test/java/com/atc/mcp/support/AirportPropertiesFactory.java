package com.atc.mcp.support;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.config.AirportProperties.OperationDuration;
import com.atc.mcp.config.AirportProperties.RunwaySpec;
import com.atc.mcp.config.AirportProperties.Separation;

import java.time.Instant;
import java.util.List;

/**
 * Builds {@link AirportProperties} for unit tests with sensible defaults.
 */
public final class AirportPropertiesFactory {

    public static final Instant DEFAULT_EPOCH = Instant.parse("2026-01-01T00:00:00Z");

    private AirportPropertiesFactory() {
    }

    public static AirportProperties defaults() {
        return new AirportProperties(
                List.of(
                        new RunwaySpec("R1", 2500),
                        new RunwaySpec("R2", 3500),
                        new RunwaySpec("R3", 4000)),
                4,
                6,
                new Separation(120, 180, 240),
                1800,
                900,
                43_200,
                new OperationDuration(600, 480),
                DEFAULT_EPOCH);
    }

    public static AirportProperties singleRunway() {
        return new AirportProperties(
                List.of(new RunwaySpec("R1", 2500)),
                2,
                4,
                new Separation(120, 180, 240),
                1800,
                900,
                43_200,
                new OperationDuration(600, 480),
                DEFAULT_EPOCH);
    }
}
