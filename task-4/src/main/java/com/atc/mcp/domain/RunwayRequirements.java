package com.atc.mcp.domain;

/**
 * Optional runway requirements declared by a flight. Only flights with a non-null
 * {@code minLengthMeters} can fail with {@code NO_SUITABLE_RUNWAY}.
 */
public record RunwayRequirements(Integer minLengthMeters) {

    public static final RunwayRequirements NONE = new RunwayRequirements(null);

    public boolean hasMinLength() {
        return minLengthMeters != null && minLengthMeters > 0;
    }
}
