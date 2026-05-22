package com.atc.mcp.mcp.dto;

import java.util.List;

public record RunwayUsageView(
        String runwayId,
        int lengthMeters,
        int scheduledOperations,
        long totalBusySeconds,
        List<TimelineEntry> operations) { }
