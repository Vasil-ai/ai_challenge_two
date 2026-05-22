package com.atc.mcp.domain;

/**
 * Concrete operation slot allocated by the scheduler. Times are seconds relative
 * to the configured {@code atc.epoch} so the engine remains deterministic and
 * easy to test.
 */
public record ScheduledOperation(
        String flightNumber,
        OperationType operationType,
        String runwayId,
        String gateId,
        long startSec,
        long endSec) {

    public long durationSec() {
        return endSec - startSec;
    }
}
