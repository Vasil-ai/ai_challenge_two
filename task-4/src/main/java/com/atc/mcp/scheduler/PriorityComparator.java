package com.atc.mcp.scheduler;

import com.atc.mcp.domain.Flight;

import java.util.Comparator;

/**
 * Total ordering used inside the scheduler. Lower-weight priority comes first
 * (HIGH before LOW) and ties break on flight number for deterministic results.
 */
public final class PriorityComparator {

    private PriorityComparator() {
    }

    public static final Comparator<Flight> INSTANCE = Comparator
            .<Flight>comparingInt(f -> f.priority().weight())
            .thenComparing(Flight::flightNumber);
}
