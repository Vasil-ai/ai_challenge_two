package com.atc.mcp.scheduler;

import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.domain.UnscheduledReason;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Output of one scheduling pass: scheduled operations, unscheduled flights with
 * their reasons, and a stable iteration order suitable for clients.
 */
public record ScheduleResult(
        Map<String, ScheduledOperation> scheduled,
        Map<String, FlightFailure> unscheduled) {

    public ScheduleResult {
        scheduled = scheduled == null ? new LinkedHashMap<>() : scheduled;
        unscheduled = unscheduled == null ? new LinkedHashMap<>() : unscheduled;
    }

    public List<ScheduledOperation> orderedOperations() {
        return scheduled.values().stream()
                .sorted((a, b) -> {
                    int byStart = Long.compare(a.startSec(), b.startSec());
                    if (byStart != 0) {
                        return byStart;
                    }
                    return a.flightNumber().compareTo(b.flightNumber());
                })
                .toList();
    }

    public record FlightFailure(UnscheduledReason reason, String details) { }
}
