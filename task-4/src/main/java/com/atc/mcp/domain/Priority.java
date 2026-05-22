package com.atc.mcp.domain;

/**
 * Flight priority. Lower {@link #weight()} is more urgent so that
 * {@code Comparator.comparingInt(Priority::weight)} sorts urgent flights first.
 */
public enum Priority {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    private final int weight;

    Priority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
