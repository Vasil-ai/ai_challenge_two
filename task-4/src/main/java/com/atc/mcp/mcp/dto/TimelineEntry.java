package com.atc.mcp.mcp.dto;

import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.ScheduledOperation;

import java.time.Instant;

public record TimelineEntry(
        String flightNumber,
        OperationType operationType,
        String runwayId,
        String gateId,
        long startSec,
        long endSec,
        Instant startAt,
        Instant endAt) {

    public static TimelineEntry from(ScheduledOperation op, Instant epoch) {
        return new TimelineEntry(
                op.flightNumber(),
                op.operationType(),
                op.runwayId(),
                op.gateId(),
                op.startSec(),
                op.endSec(),
                epoch.plusSeconds(op.startSec()),
                epoch.plusSeconds(op.endSec()));
    }
}
