package com.atc.mcp.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Mutable in-memory representation of a flight. Mutability is intentional and
 * confined to the AirportStateStore which owns all writes under a write lock.
 * Snapshots returned to the outside world are converted into immutable views.
 */
public final class Flight {

    private final String flightNumber;
    private final OperationType operationType;
    private final Priority priority;
    private final List<String> dependencies;
    private final RunwayRequirements runwayRequirements;
    private final long createdAtMonoNanos;

    private FlightStatus status = FlightStatus.PENDING;
    private UnscheduledReason unscheduledReason;
    private String unscheduledDetails;
    private String assignedRunwayId;
    private String assignedGateId;
    private Long scheduledStartSec;
    private Long scheduledEndSec;

    public Flight(String flightNumber,
                  OperationType operationType,
                  Priority priority,
                  List<String> dependencies,
                  RunwayRequirements runwayRequirements,
                  long createdAtMonoNanos) {
        this.flightNumber = Objects.requireNonNull(flightNumber, "flightNumber");
        this.operationType = Objects.requireNonNull(operationType, "operationType");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
        this.runwayRequirements = Objects.requireNonNullElse(runwayRequirements, RunwayRequirements.NONE);
        this.createdAtMonoNanos = createdAtMonoNanos;
    }

    public String flightNumber() {
        return flightNumber;
    }

    public OperationType operationType() {
        return operationType;
    }

    public Priority priority() {
        return priority;
    }

    public List<String> dependencies() {
        return dependencies;
    }

    public Set<String> dependencySet() {
        return Set.copyOf(dependencies);
    }

    public RunwayRequirements runwayRequirements() {
        return runwayRequirements;
    }

    public long createdAtMonoNanos() {
        return createdAtMonoNanos;
    }

    public FlightStatus status() {
        return status;
    }

    public UnscheduledReason unscheduledReason() {
        return unscheduledReason;
    }

    public String unscheduledDetails() {
        return unscheduledDetails;
    }

    public String assignedRunwayId() {
        return assignedRunwayId;
    }

    public String assignedGateId() {
        return assignedGateId;
    }

    public Long scheduledStartSec() {
        return scheduledStartSec;
    }

    public Long scheduledEndSec() {
        return scheduledEndSec;
    }

    public void markScheduled(String runwayId, String gateId, long startSec, long endSec) {
        this.status = FlightStatus.SCHEDULED;
        this.assignedRunwayId = runwayId;
        this.assignedGateId = gateId;
        this.scheduledStartSec = startSec;
        this.scheduledEndSec = endSec;
        this.unscheduledReason = null;
        this.unscheduledDetails = null;
    }

    public void markUnscheduled(UnscheduledReason reason, String details) {
        this.status = FlightStatus.UNSCHEDULED;
        this.unscheduledReason = Objects.requireNonNull(reason, "reason");
        this.unscheduledDetails = details;
        clearAssignment();
    }

    public void markCancelled() {
        this.status = FlightStatus.CANCELLED;
        clearAssignment();
        this.unscheduledReason = null;
        this.unscheduledDetails = null;
    }

    public void resetToPending() {
        this.status = FlightStatus.PENDING;
        clearAssignment();
        this.unscheduledReason = null;
        this.unscheduledDetails = null;
    }

    private void clearAssignment() {
        this.assignedRunwayId = null;
        this.assignedGateId = null;
        this.scheduledStartSec = null;
        this.scheduledEndSec = null;
    }
}
