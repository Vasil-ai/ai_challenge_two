package com.atc.mcp.scheduler;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.config.AirportProperties.RunwaySpec;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.domain.UnscheduledReason;
import com.atc.mcp.scheduler.ScheduleResult.FlightFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure scheduler: given the current flight list and configuration, computes a
 * fresh schedule. The engine is deterministic — it never reads the wall clock.
 *
 * <p>Algorithm</p>
 * <ol>
 *   <li>Resolve dependencies into a priority-aware topological order
 *       ({@link DependencyResolver}). Cycles and missing/cancelled dependencies
 *       are excluded with reasons.</li>
 *   <li>For each flight, compute its earliest possible start: zero, or the max
 *       of {@code dependency.endSec + dependencyBufferSeconds}.</li>
 *   <li>Pick the runway that satisfies the runway requirement and offers the
 *       earliest feasible slot honoring separation buffers and a free gate
 *       window. Prefer the smallest-fitting runway as a deterministic tiebreak.</li>
 *   <li>Mark unscheduled flights with one of the
 *       {@link UnscheduledReason} codes.</li>
 * </ol>
 */
@Component
public class SchedulingEngine {

    private final AirportProperties config;

    public SchedulingEngine(AirportProperties config) {
        this.config = config;
    }

    public ScheduleResult schedule(List<Flight> activeFlights, Set<String> cancelledFlightNumbers) {
        DependencyResolver.Result resolution =
                DependencyResolver.resolve(activeFlights, cancelledFlightNumbers);

        List<RunwaySpec> runwaysByLength = config.runways().stream()
                .sorted(Comparator
                        .comparingInt(RunwaySpec::lengthMeters)
                        .thenComparing(RunwaySpec::id))
                .toList();
        List<String> gateIds = generateGateIds(config.gateCount());

        Map<String, List<RunwayBooking>> runwayBookings = new LinkedHashMap<>();
        for (RunwaySpec runway : runwaysByLength) {
            runwayBookings.put(runway.id(), new ArrayList<>());
        }
        Map<String, List<long[]>> gateBookings = new LinkedHashMap<>();
        for (String gate : gateIds) {
            gateBookings.put(gate, new ArrayList<>());
        }

        Map<String, ScheduledOperation> scheduled = new LinkedHashMap<>();
        Map<String, FlightFailure> unscheduled = new LinkedHashMap<>();

        for (Map.Entry<String, UnscheduledReason> entry : resolution.excluded().entrySet()) {
            String fn = entry.getKey();
            unscheduled.put(fn, new FlightFailure(entry.getValue(),
                    resolution.excludedDetails().getOrDefault(fn, entry.getValue().description())));
        }

        for (Flight flight : resolution.ordered()) {
            if (flight.status() == FlightStatus.CANCELLED) {
                continue;
            }

            long earliestStart = computeEarliestStart(flight, scheduled);
            long duration = operationDuration(flight.operationType());

            List<RunwaySpec> candidates = runwaysByLength.stream()
                    .filter(r -> meetsRequirements(r, flight))
                    .toList();
            if (candidates.isEmpty()) {
                unscheduled.put(flight.flightNumber(), new FlightFailure(
                        UnscheduledReason.NO_SUITABLE_RUNWAY,
                        describeNoSuitableRunway(flight)));
                continue;
            }

            BestSlot bestSlot = null;
            for (RunwaySpec runway : candidates) {
                BestSlot slot = findEarliestSlot(
                        runway, runwayBookings.get(runway.id()), flight, earliestStart, duration,
                        gateBookings);
                if (slot == null) {
                    continue;
                }
                if (bestSlot == null
                        || slot.startSec < bestSlot.startSec
                        || (slot.startSec == bestSlot.startSec
                            && runway.lengthMeters() < bestSlot.runwayLengthMeters)) {
                    bestSlot = new BestSlot(runway.id(), runway.lengthMeters(),
                            slot.gateId, slot.startSec, slot.startSec + duration);
                }
            }

            if (bestSlot == null) {
                UnscheduledReason reason = inferFailureReason(candidates, runwayBookings, gateBookings,
                        flight, earliestStart, duration);
                unscheduled.put(flight.flightNumber(), new FlightFailure(reason,
                        reason.description()));
                continue;
            }

            ScheduledOperation op = new ScheduledOperation(
                    flight.flightNumber(), flight.operationType(),
                    bestSlot.runwayId, bestSlot.gateId,
                    bestSlot.startSec, bestSlot.endSec);
            scheduled.put(flight.flightNumber(), op);
            runwayBookings.get(bestSlot.runwayId).add(new RunwayBooking(
                    bestSlot.startSec, bestSlot.endSec, flight.operationType()));
            runwayBookings.get(bestSlot.runwayId)
                    .sort(Comparator.comparingLong(RunwayBooking::startSec));
            gateBookings.get(bestSlot.gateId).add(new long[] {
                    bestSlot.startSec, bestSlot.endSec + config.gateTurnaroundSeconds()
            });
            gateBookings.get(bestSlot.gateId)
                    .sort(Comparator.comparingLong(a -> a[0]));
        }

        return new ScheduleResult(scheduled, unscheduled);
    }

    private long computeEarliestStart(Flight flight, Map<String, ScheduledOperation> scheduled) {
        long earliest = 0L;
        for (String dep : flight.dependencies()) {
            ScheduledOperation depOp = scheduled.get(dep);
            if (depOp == null) {
                continue;
            }
            earliest = Math.max(earliest, depOp.endSec() + config.dependencyBufferSeconds());
        }
        return earliest;
    }

    private long operationDuration(OperationType type) {
        return type.isArrival()
                ? config.operationDuration().arrivalSeconds()
                : config.operationDuration().departureSeconds();
    }

    private boolean meetsRequirements(RunwaySpec runway, Flight flight) {
        if (!flight.runwayRequirements().hasMinLength()) {
            return true;
        }
        return runway.lengthMeters() >= flight.runwayRequirements().minLengthMeters();
    }

    private String describeNoSuitableRunway(Flight flight) {
        Integer min = flight.runwayRequirements().minLengthMeters();
        return "No runway satisfies minLengthMeters=" + min;
    }

    private BestSlot findEarliestSlot(RunwaySpec runway,
                                      List<RunwayBooking> bookings,
                                      Flight flight,
                                      long earliestStart,
                                      long duration,
                                      Map<String, List<long[]>> gateBookings) {
        long horizon = config.schedulingHorizonSeconds();
        long candidateStart = earliestStart;

        List<RunwayBooking> sorted = new ArrayList<>(bookings);
        sorted.sort(Comparator.comparingLong(RunwayBooking::startSec));

        for (int idx = 0; idx <= sorted.size(); idx++) {
            long windowStart;
            long windowEnd;
            if (idx == 0) {
                windowStart = candidateStart;
            } else {
                RunwayBooking prev = sorted.get(idx - 1);
                long sep = config.separation().forTransition(
                        prev.operationType().isArrival(), flight.operationType().isArrival());
                windowStart = Math.max(candidateStart, prev.endSec() + sep);
            }
            if (idx < sorted.size()) {
                RunwayBooking next = sorted.get(idx);
                long sepToNext = config.separation().forTransition(
                        flight.operationType().isArrival(), next.operationType().isArrival());
                windowEnd = next.startSec() - sepToNext;
            } else {
                windowEnd = horizon;
            }

            if (windowEnd < windowStart + duration) {
                continue;
            }

            long attempt = windowStart;
            while (attempt + duration <= windowEnd) {
                String gate = chooseGateAt(attempt, attempt + duration, gateBookings);
                if (gate != null) {
                    return new BestSlot(runway.id(), runway.lengthMeters(),
                            gate, attempt, attempt + duration);
                }
                long nextGateFree = nextGateAvailability(attempt, attempt + duration, gateBookings);
                if (nextGateFree <= attempt) {
                    break;
                }
                attempt = nextGateFree;
            }
        }
        return null;
    }

    private String chooseGateAt(long startSec, long endSec, Map<String, List<long[]>> gateBookings) {
        for (Map.Entry<String, List<long[]>> entry : gateBookings.entrySet()) {
            if (gateFreeDuring(entry.getValue(), startSec, endSec)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean gateFreeDuring(List<long[]> bookings, long startSec, long endSec) {
        for (long[] busy : bookings) {
            long busyStart = busy[0];
            long busyEnd = busy[1];
            if (busyStart < endSec && startSec < busyEnd) {
                return false;
            }
        }
        return true;
    }

    private long nextGateAvailability(long startSec, long endSec, Map<String, List<long[]>> gateBookings) {
        long earliestRelease = Long.MAX_VALUE;
        for (List<long[]> bookings : gateBookings.values()) {
            long latestOverlapEnd = startSec;
            boolean anyOverlap = false;
            for (long[] busy : bookings) {
                if (busy[0] < endSec && startSec < busy[1]) {
                    anyOverlap = true;
                    if (busy[1] > latestOverlapEnd) {
                        latestOverlapEnd = busy[1];
                    }
                }
            }
            if (!anyOverlap) {
                return startSec;
            }
            if (latestOverlapEnd < earliestRelease) {
                earliestRelease = latestOverlapEnd;
            }
        }
        return earliestRelease;
    }

    private UnscheduledReason inferFailureReason(List<RunwaySpec> candidates,
                                                 Map<String, List<RunwayBooking>> runwayBookings,
                                                 Map<String, List<long[]>> gateBookings,
                                                 Flight flight,
                                                 long earliestStart,
                                                 long duration) {
        long horizon = config.schedulingHorizonSeconds();
        boolean anyRunwaySlotFound = false;
        for (RunwaySpec runway : candidates) {
            BestSlot slot = findEarliestSlotIgnoringGate(runway, runwayBookings.get(runway.id()),
                    flight, earliestStart, duration, horizon);
            if (slot != null) {
                anyRunwaySlotFound = true;
                break;
            }
        }
        if (!anyRunwaySlotFound) {
            return UnscheduledReason.NO_SLOT_WITHIN_HORIZON;
        }

        Set<String> usedGates = new HashSet<>();
        for (List<long[]> bookings : gateBookings.values()) {
            if (!bookings.isEmpty()) {
                usedGates.add("g");
            }
        }
        return UnscheduledReason.NO_GATE_AVAILABLE;
    }

    private BestSlot findEarliestSlotIgnoringGate(RunwaySpec runway,
                                                  List<RunwayBooking> bookings,
                                                  Flight flight,
                                                  long earliestStart,
                                                  long duration,
                                                  long horizon) {
        List<RunwayBooking> sorted = new ArrayList<>(bookings);
        sorted.sort(Comparator.comparingLong(RunwayBooking::startSec));
        for (int idx = 0; idx <= sorted.size(); idx++) {
            long windowStart;
            long windowEnd;
            if (idx == 0) {
                windowStart = earliestStart;
            } else {
                RunwayBooking prev = sorted.get(idx - 1);
                long sep = config.separation().forTransition(
                        prev.operationType().isArrival(), flight.operationType().isArrival());
                windowStart = Math.max(earliestStart, prev.endSec() + sep);
            }
            if (idx < sorted.size()) {
                RunwayBooking next = sorted.get(idx);
                long sepToNext = config.separation().forTransition(
                        flight.operationType().isArrival(), next.operationType().isArrival());
                windowEnd = next.startSec() - sepToNext;
            } else {
                windowEnd = horizon;
            }
            if (windowEnd >= windowStart + duration) {
                return new BestSlot(runway.id(), runway.lengthMeters(), null,
                        windowStart, windowStart + duration);
            }
        }
        return null;
    }

    private List<String> generateGateIds(int count) {
        List<String> gates = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            gates.add(String.format("G%02d", i));
        }
        return gates;
    }

    private record RunwayBooking(long startSec, long endSec, OperationType operationType) { }

    private record BestSlot(String runwayId,
                            int runwayLengthMeters,
                            String gateId,
                            long startSec,
                            long endSec) { }
}
