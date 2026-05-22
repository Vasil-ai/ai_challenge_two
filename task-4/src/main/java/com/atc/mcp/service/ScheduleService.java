package com.atc.mcp.service;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.scheduler.ScheduleResult;
import com.atc.mcp.scheduler.ScheduleResult.FlightFailure;
import com.atc.mcp.scheduler.SchedulingEngine;
import com.atc.mcp.store.AirportStateStore;
import com.atc.mcp.store.AirportStateStore.ScheduleApplication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Atomically replaces the in-memory schedule by invoking the scheduling engine
 * with a snapshot of the current store, then writing the resulting state back.
 */
@Service
public class ScheduleService {

    private final SchedulingEngine engine;
    private final AirportStateStore store;
    private final AirportProperties properties;

    public ScheduleService(SchedulingEngine engine,
                           AirportStateStore store,
                           AirportProperties properties) {
        this.engine = engine;
        this.store = store;
        this.properties = properties;
    }

    public ScheduleSummary regenerate() {
        return store.applyScheduleResult(flights -> {
            List<Flight> active = new ArrayList<>();
            Set<String> cancelled = new HashSet<>();
            for (Flight flight : flights.values()) {
                if (flight.status() == FlightStatus.CANCELLED) {
                    cancelled.add(flight.flightNumber());
                } else {
                    flight.resetToPending();
                    active.add(flight);
                }
            }

            ScheduleResult result = engine.schedule(active, cancelled);

            for (Flight flight : flights.values()) {
                if (flight.status() == FlightStatus.CANCELLED) {
                    continue;
                }
                ScheduledOperation op = result.scheduled().get(flight.flightNumber());
                if (op != null) {
                    flight.markScheduled(op.runwayId(), op.gateId(), op.startSec(), op.endSec());
                    continue;
                }
                FlightFailure failure = result.unscheduled().get(flight.flightNumber());
                if (failure != null) {
                    flight.markUnscheduled(failure.reason(), failure.details());
                }
            }

            Map<String, ScheduledOperation> ordered = new LinkedHashMap<>();
            for (ScheduledOperation op : result.orderedOperations()) {
                ordered.put(op.flightNumber(), op);
            }

            ScheduleSummary summary = buildSummary(flights);
            return new ScheduleApplication<>(ordered, summary);
        });
    }

    private ScheduleSummary buildSummary(Map<String, Flight> flights) {
        List<FlightView> scheduled = new ArrayList<>();
        List<FlightView> unscheduled = new ArrayList<>();
        List<FlightView> cancelled = new ArrayList<>();
        for (Flight flight : flights.values()) {
            FlightView view = FlightView.from(flight, properties.epoch());
            switch (flight.status()) {
                case SCHEDULED -> scheduled.add(view);
                case UNSCHEDULED -> unscheduled.add(view);
                case CANCELLED -> cancelled.add(view);
                case PENDING -> { /* no flights remain pending after a regeneration */ }
            }
        }
        scheduled.sort((a, b) -> Long.compare(
                a.scheduledStartSec() == null ? Long.MAX_VALUE : a.scheduledStartSec(),
                b.scheduledStartSec() == null ? Long.MAX_VALUE : b.scheduledStartSec()));
        unscheduled.sort((a, b) -> a.flightNumber().compareTo(b.flightNumber()));
        cancelled.sort((a, b) -> a.flightNumber().compareTo(b.flightNumber()));
        return new ScheduleSummary(scheduled, unscheduled, cancelled);
    }

    public record ScheduleSummary(List<FlightView> scheduled,
                                  List<FlightView> unscheduled,
                                  List<FlightView> cancelled) { }
}
