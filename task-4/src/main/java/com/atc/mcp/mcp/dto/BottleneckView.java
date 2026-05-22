package com.atc.mcp.mcp.dto;

import java.time.Instant;
import java.util.List;

public record BottleneckView(
        boolean exists,
        List<String> flightNumbers,
        long totalDurationSeconds,
        long startSec,
        long endSec,
        Instant startAt,
        Instant endAt,
        String message) {

    public static BottleneckView empty(String message) {
        return new BottleneckView(false, List.of(), 0L, 0L, 0L, null, null, message);
    }
}
