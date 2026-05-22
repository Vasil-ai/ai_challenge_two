package com.atc.mcp.mcp.dto;

import java.util.List;

public record FlightQueueView(
        List<FlightView> scheduled,
        List<FlightView> unscheduled,
        List<FlightView> pending,
        List<FlightView> cancelled) { }
