package com.atc.mcp.mcp.dto;

import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.domain.UnscheduledReason;

import java.time.Instant;
import java.util.List;

/**
 * Immutable JSON-friendly projection of a {@link Flight}.
 */
public record FlightView(
        String flightNumber,
        OperationType operationType,
        Priority priority,
        FlightStatus status,
        List<String> dependencies,
        Integer minRunwayLengthMeters,
        UnscheduledReason unscheduledReason,
        String unscheduledDetails,
        String assignedRunwayId,
        String assignedGateId,
        Long scheduledStartSec,
        Long scheduledEndSec,
        Instant scheduledStartAt,
        Instant scheduledEndAt) {

    public static FlightView from(Flight flight, Instant epoch) {
        Instant startAt = flight.scheduledStartSec() != null
                ? epoch.plusSeconds(flight.scheduledStartSec()) : null;
        Instant endAt = flight.scheduledEndSec() != null
                ? epoch.plusSeconds(flight.scheduledEndSec()) : null;
        return new FlightView(
                flight.flightNumber(),
                flight.operationType(),
                flight.priority(),
                flight.status(),
                flight.dependencies(),
                flight.runwayRequirements().minLengthMeters(),
                flight.unscheduledReason(),
                flight.unscheduledDetails(),
                flight.assignedRunwayId(),
                flight.assignedGateId(),
                flight.scheduledStartSec(),
                flight.scheduledEndSec(),
                startAt,
                endAt);
    }
}
