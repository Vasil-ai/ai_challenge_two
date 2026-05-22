package com.atc.mcp.scheduler;

import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.UnscheduledReason;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Performs a priority-aware topological sort of the input flights:
 * <ul>
 *   <li>flights whose dependencies are missing or cancelled are excluded with a reason;</li>
 *   <li>flights participating in a cycle are excluded with a reason;</li>
 *   <li>all remaining flights are returned in an order that respects dependencies
 *       and biases toward higher-priority (lower-weight) flights when free.</li>
 * </ul>
 */
public final class DependencyResolver {

    private DependencyResolver() {
    }

    public static Result resolve(List<Flight> input, Set<String> cancelledFlightNumbers) {
        Map<String, Flight> byNumber = new LinkedHashMap<>();
        for (Flight f : input) {
            byNumber.put(f.flightNumber(), f);
        }

        Map<String, UnscheduledReason> excluded = new LinkedHashMap<>();
        Map<String, String> excludedDetails = new HashMap<>();

        for (Flight f : input) {
            for (String dep : f.dependencies()) {
                if (!byNumber.containsKey(dep) && !cancelledFlightNumbers.contains(dep)) {
                    excluded.put(f.flightNumber(), UnscheduledReason.DEPENDENCY_MISSING);
                    excludedDetails.put(f.flightNumber(), "Unknown dependency '" + dep + "'");
                    break;
                }
                if (cancelledFlightNumbers.contains(dep)) {
                    excluded.put(f.flightNumber(), UnscheduledReason.DEPENDENCY_CANCELLED);
                    excludedDetails.put(f.flightNumber(), "Dependency '" + dep + "' was cancelled");
                    break;
                }
            }
        }

        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (Flight f : input) {
            if (excluded.containsKey(f.flightNumber())) {
                continue;
            }
            indegree.putIfAbsent(f.flightNumber(), 0);
            for (String dep : f.dependencies()) {
                if (excluded.containsKey(dep)) {
                    excluded.put(f.flightNumber(), UnscheduledReason.DEPENDENCY_UNSCHEDULED);
                    excludedDetails.put(f.flightNumber(),
                            "Dependency '" + dep + "' could not be scheduled");
                    indegree.remove(f.flightNumber());
                    break;
                }
                indegree.merge(f.flightNumber(), 1, Integer::sum);
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(f.flightNumber());
            }
        }

        PriorityQueue<Flight> ready = new PriorityQueue<>(PriorityComparator.INSTANCE);
        for (Flight f : input) {
            if (!excluded.containsKey(f.flightNumber()) && indegree.getOrDefault(f.flightNumber(), 0) == 0) {
                ready.add(f);
            }
        }

        List<Flight> ordered = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        while (!ready.isEmpty()) {
            Flight next = ready.poll();
            if (!emitted.add(next.flightNumber())) {
                continue;
            }
            ordered.add(next);
            for (String child : dependents.getOrDefault(next.flightNumber(), List.of())) {
                Integer remaining = indegree.computeIfPresent(child, (k, v) -> v - 1);
                if (remaining != null && remaining == 0) {
                    Flight childFlight = byNumber.get(child);
                    if (childFlight != null && !excluded.containsKey(child)) {
                        ready.add(childFlight);
                    }
                }
            }
        }

        for (Flight f : input) {
            String number = f.flightNumber();
            if (excluded.containsKey(number) || emitted.contains(number)) {
                continue;
            }
            excluded.put(number, UnscheduledReason.DEPENDENCY_CYCLE);
            excludedDetails.put(number, "Flight participates in a dependency cycle");
        }

        return new Result(ordered, excluded, excludedDetails);
    }

    public record Result(
            List<Flight> ordered,
            Map<String, UnscheduledReason> excluded,
            Map<String, String> excludedDetails) { }
}
