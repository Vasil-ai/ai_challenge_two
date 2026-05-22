package com.atc.mcp.service;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.config.AirportProperties.RunwaySpec;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.mcp.dto.AirportStatusView;
import com.atc.mcp.mcp.dto.AirportStatusView.RunwayCapacityView;
import com.atc.mcp.mcp.dto.AirportStatusView.UnscheduledFlightView;
import com.atc.mcp.mcp.dto.FlightQueueView;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.mcp.dto.RunwayUsageView;
import com.atc.mcp.mcp.dto.TimelineEntry;
import com.atc.mcp.scheduler.BottleneckAnalyzer;
import com.atc.mcp.scheduler.BottleneckAnalyzer.BottleneckChain;
import com.atc.mcp.mcp.dto.BottleneckView;
import com.atc.mcp.store.AirportStateStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds JSON-friendly views over the airport state used by both MCP tools and
 * MCP resources.
 */
@Service
public class StatusService {

    private final AirportStateStore store;
    private final AirportProperties properties;
    private final BottleneckAnalyzer bottleneckAnalyzer;

    public StatusService(AirportStateStore store,
                         AirportProperties properties,
                         BottleneckAnalyzer bottleneckAnalyzer) {
        this.store = store;
        this.properties = properties;
        this.bottleneckAnalyzer = bottleneckAnalyzer;
    }

    public AirportStatusView buildStatus() {
        return store.readWith(view -> {
            Map<String, Integer> byStatus = new LinkedHashMap<>();
            for (FlightStatus status : FlightStatus.values()) {
                byStatus.put(status.name(), 0);
            }
            Map<String, Integer> byOperation = new LinkedHashMap<>();
            for (OperationType type : OperationType.values()) {
                byOperation.put(type.name(), 0);
            }

            int totalFlights = 0;
            List<UnscheduledFlightView> unscheduled = new ArrayList<>();
            for (Flight flight : view.flights().values()) {
                totalFlights++;
                byStatus.merge(flight.status().name(), 1, Integer::sum);
                byOperation.merge(flight.operationType().name(), 1, Integer::sum);
                if (flight.status() == FlightStatus.UNSCHEDULED) {
                    unscheduled.add(new UnscheduledFlightView(
                            flight.flightNumber(),
                            flight.unscheduledReason(),
                            flight.unscheduledDetails()));
                }
            }
            unscheduled.sort(Comparator.comparing(UnscheduledFlightView::flightNumber));

            Map<String, RunwayCapacityView> capacity = new LinkedHashMap<>();
            Map<String, Long> runwayBusy = new LinkedHashMap<>();
            Map<String, Integer> runwayOpsCount = new LinkedHashMap<>();
            Map<String, Long> gateBusy = new LinkedHashMap<>();
            for (RunwaySpec runway : properties.runways()) {
                runwayBusy.put(runway.id(), 0L);
                runwayOpsCount.put(runway.id(), 0);
            }
            int gateUsageMax = 0;
            long scheduleCompletion = 0L;
            for (ScheduledOperation op : view.schedule().values()) {
                runwayBusy.merge(op.runwayId(), op.durationSec(), Long::sum);
                runwayOpsCount.merge(op.runwayId(), 1, Integer::sum);
                gateBusy.merge(op.gateId(), op.durationSec(), Long::sum);
                if (op.endSec() > scheduleCompletion) {
                    scheduleCompletion = op.endSec();
                }
            }
            for (RunwaySpec runway : properties.runways()) {
                capacity.put(runway.id(), new RunwayCapacityView(
                        runway.id(),
                        runway.lengthMeters(),
                        runwayOpsCount.getOrDefault(runway.id(), 0),
                        runwayBusy.getOrDefault(runway.id(), 0L)));
            }
            gateUsageMax = gateBusy.size();

            int runwayUsageMax = (int) runwayOpsCount.values().stream().filter(c -> c > 0).count();
            boolean resourceConstrained = !unscheduled.isEmpty()
                    || runwayUsageMax >= properties.runways().size()
                    || gateUsageMax >= properties.gateCount();

            Instant completionAt = scheduleCompletion > 0
                    ? properties.epoch().plusSeconds(scheduleCompletion)
                    : null;

            return new AirportStatusView(
                    byStatus,
                    byOperation,
                    totalFlights,
                    properties.runways().size(),
                    properties.gateCount(),
                    properties.groundCrewCount(),
                    gateUsageMax,
                    runwayUsageMax,
                    scheduleCompletion,
                    completionAt,
                    resourceConstrained,
                    unscheduled,
                    new ArrayList<>(capacity.values()));
        });
    }

    public FlightQueueView buildQueue() {
        return store.readWith(view -> {
            List<FlightView> scheduled = new ArrayList<>();
            List<FlightView> unscheduled = new ArrayList<>();
            List<FlightView> pending = new ArrayList<>();
            List<FlightView> cancelled = new ArrayList<>();
            for (Flight flight : view.flights().values()) {
                FlightView projected = FlightView.from(flight, properties.epoch());
                switch (flight.status()) {
                    case SCHEDULED -> scheduled.add(projected);
                    case UNSCHEDULED -> unscheduled.add(projected);
                    case PENDING -> pending.add(projected);
                    case CANCELLED -> cancelled.add(projected);
                }
            }
            scheduled.sort((a, b) -> Long.compare(
                    a.scheduledStartSec() == null ? Long.MAX_VALUE : a.scheduledStartSec(),
                    b.scheduledStartSec() == null ? Long.MAX_VALUE : b.scheduledStartSec()));
            unscheduled.sort(Comparator.comparing(FlightView::flightNumber));
            pending.sort(Comparator.comparing(FlightView::flightNumber));
            cancelled.sort(Comparator.comparing(FlightView::flightNumber));
            return new FlightQueueView(scheduled, unscheduled, pending, cancelled);
        });
    }

    public List<RunwayUsageView> buildRunwayUsage() {
        return store.readWith(view -> {
            Map<String, List<TimelineEntry>> opsByRunway = new LinkedHashMap<>();
            for (RunwaySpec runway : properties.runways()) {
                opsByRunway.put(runway.id(), new ArrayList<>());
            }
            for (ScheduledOperation op : view.schedule().values()) {
                opsByRunway.computeIfAbsent(op.runwayId(), k -> new ArrayList<>())
                        .add(TimelineEntry.from(op, properties.epoch()));
            }
            List<RunwayUsageView> result = new ArrayList<>();
            for (RunwaySpec runway : properties.runways()) {
                List<TimelineEntry> ops = opsByRunway.getOrDefault(runway.id(), List.of());
                ops.sort(Comparator.comparingLong(TimelineEntry::startSec));
                long busy = ops.stream().mapToLong(t -> t.endSec() - t.startSec()).sum();
                result.add(new RunwayUsageView(
                        runway.id(),
                        runway.lengthMeters(),
                        ops.size(),
                        busy,
                        ops));
            }
            return result;
        });
    }

    public List<TimelineEntry> buildTimeline() {
        return store.readWith(view -> view.schedule().values().stream()
                .sorted(Comparator
                        .comparingLong(ScheduledOperation::startSec)
                        .thenComparing(ScheduledOperation::flightNumber))
                .map(op -> TimelineEntry.from(op, properties.epoch()))
                .toList());
    }

    public BottleneckView analyzeBottleneck() {
        return store.readWith(view -> {
            Optional<BottleneckChain> chainOpt = bottleneckAnalyzer.analyze(
                    new ArrayList<>(view.flights().values()),
                    new LinkedHashMap<>(view.schedule()));
            if (chainOpt.isEmpty()) {
                return BottleneckView.empty(
                        "No scheduled dependency chain of length >= 2 currently exists");
            }
            BottleneckChain chain = chainOpt.get();
            return new BottleneckView(
                    true,
                    chain.flightNumbers(),
                    chain.totalDurationSeconds(),
                    chain.startSec(),
                    chain.endSec(),
                    properties.epoch().plusSeconds(chain.startSec()),
                    properties.epoch().plusSeconds(chain.endSec()),
                    "Longest active scheduled dependency chain");
        });
    }
}
