package com.atc.mcp.config;

import com.atc.mcp.config.AirportProperties.RunwaySpec;
import com.atc.mcp.exception.InvalidConfigurationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cross-field validator for {@link AirportProperties} executed on startup.
 * Throws {@link InvalidConfigurationException} with a human-readable message so
 * that boot fails clearly when the airport is mis-configured.
 */
@Component
public class AirportPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(AirportPropertiesValidator.class);

    private final AirportProperties properties;

    public AirportPropertiesValidator(AirportProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateOnStartup() {
        List<String> errors = new ArrayList<>();

        validateRunways(errors);
        validateHorizonVsOperationDurations(errors);

        if (!errors.isEmpty()) {
            String joined = String.join("; ", errors);
            log.error("Invalid airport configuration: {}", joined);
            throw new InvalidConfigurationException(joined);
        }

        log.info("Airport configuration validated. runways={}, gates={}, groundCrew={}, horizon={}s",
                properties.runways().size(),
                properties.gateCount(),
                properties.groundCrewCount(),
                properties.schedulingHorizonSeconds());
    }

    private void validateRunways(List<String> errors) {
        if (properties.runways() == null || properties.runways().isEmpty()) {
            errors.add("ATC_RUNWAYS must contain at least one runway");
            return;
        }
        Set<String> seenIds = new HashSet<>();
        for (RunwaySpec spec : properties.runways()) {
            if (spec.id() == null || spec.id().isBlank()) {
                errors.add("Runway id must be non-blank");
                continue;
            }
            if (!seenIds.add(spec.id())) {
                errors.add("Duplicate runway id '" + spec.id() + "'");
            }
            if (spec.lengthMeters() <= 0) {
                errors.add("Runway '" + spec.id() + "' must have positive length");
            }
        }
    }

    private void validateHorizonVsOperationDurations(List<String> errors) {
        long horizon = properties.schedulingHorizonSeconds();
        long maxOp = Math.max(
                properties.operationDuration().arrivalSeconds(),
                properties.operationDuration().departureSeconds());
        if (horizon < maxOp) {
            errors.add("ATC_SCHEDULING_HORIZON_SECONDS (" + horizon
                    + ") must be >= longest operation duration (" + maxOp + ")");
        }
    }
}
