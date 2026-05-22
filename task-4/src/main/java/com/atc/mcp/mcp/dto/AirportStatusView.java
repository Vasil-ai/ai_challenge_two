package com.atc.mcp.mcp.dto;

import com.atc.mcp.domain.UnscheduledReason;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AirportStatusView(
        Map<String, Integer> flightCountsByStatus,
        Map<String, Integer> flightCountsByOperationType,
        int totalFlights,
        int runwayCount,
        int gateCount,
        int groundCrewCount,
        int gateUsageMax,
        int runwayUsageMax,
        long scheduleCompletionSec,
        Instant scheduleCompletionAt,
        boolean resourceConstrained,
        List<UnscheduledFlightView> unscheduledFlights,
        List<RunwayCapacityView> runwayCapacity) {

    public record UnscheduledFlightView(
            String flightNumber,
            UnscheduledReason reason,
            String details) { }

    public record RunwayCapacityView(
            String runwayId,
            int lengthMeters,
            int scheduledOperations,
            long busySeconds) { }
}
