package com.atc.mcp.exception;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Translates an {@link InvalidConfigurationException} thrown at startup into a
 * Spring Boot {@link FailureAnalysis} so the user sees a concise, actionable
 * message instead of a full stack trace.
 */
public class InvalidConfigurationFailureAnalyzer
        extends AbstractFailureAnalyzer<InvalidConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationException cause) {
        String description = "Invalid airport configuration: " + cause.getMessage();
        String action = "Review the ATC_* environment variables. "
                + "ATC_RUNWAYS must be a non-empty comma-separated list of `id:lengthMeters`, "
                + "all counts and durations must be positive (or non-negative where indicated), "
                + "and ATC_SCHEDULING_HORIZON_SECONDS must be at least as large as the longest "
                + "operation duration. See README.md for the complete environment-variable reference.";
        return new FailureAnalysis(description, action, cause);
    }
}
