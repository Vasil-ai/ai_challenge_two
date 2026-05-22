package com.atc.mcp.domain;

/**
 * Stable, machine-readable codes explaining why a flight could not be scheduled.
 * Pair each enum constant with a human-readable description for clients.
 */
public enum UnscheduledReason {

    NO_SUITABLE_RUNWAY("No runway satisfies the flight runway requirements"),
    NO_GATE_AVAILABLE("No gate is available within the scheduling horizon"),
    NO_SLOT_WITHIN_HORIZON("No runway slot fits within the scheduling horizon"),
    DEPENDENCY_UNSCHEDULED("One or more dependency flights could not be scheduled"),
    DEPENDENCY_CYCLE("Flight participates in a dependency cycle"),
    DEPENDENCY_MISSING("A declared dependency flight does not exist"),
    DEPENDENCY_CANCELLED("A dependency flight has been cancelled");

    private final String description;

    UnscheduledReason(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
