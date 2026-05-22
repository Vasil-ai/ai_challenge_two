package com.atc.mcp.scheduler;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.ScheduledOperation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes the longest dependency chain among scheduled flights using a
 * topological-order DP. The total elapsed duration accounts for each flight's
 * own operation duration plus the configured dependency buffer between adjacent
 * scheduled flights in the chain.
 */
@Component
public class BottleneckAnalyzer {

    private final AirportProperties config;

    public BottleneckAnalyzer(AirportProperties config) {
        this.config = config;
    }

    public Optional<BottleneckChain> analyze(List<Flight> flights,
                                             Map<String, ScheduledOperation> schedule) {
        Map<String, Flight> active = new LinkedHashMap<>();
        for (Flight f : flights) {
            if (f.status() == FlightStatus.SCHEDULED && schedule.containsKey(f.flightNumber())) {
                active.put(f.flightNumber(), f);
            }
        }
        if (active.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Long> longestPathLength = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        List<String> ordered = topologicalOrder(active);
        long buffer = config.dependencyBufferSeconds();

        for (String number : ordered) {
            Flight flight = active.get(number);
            ScheduledOperation op = schedule.get(number);
            long ownDuration = op.durationSec();

            String bestPrev = null;
            long bestLength = ownDuration;
            for (String dep : flight.dependencies()) {
                if (!active.containsKey(dep)) {
                    continue;
                }
                long candidate = longestPathLength.getOrDefault(dep, 0L) + buffer + ownDuration;
                if (candidate > bestLength
                        || (candidate == bestLength && bestPrev != null && dep.compareTo(bestPrev) < 0)) {
                    bestLength = candidate;
                    bestPrev = dep;
                }
            }
            longestPathLength.put(number, bestLength);
            if (bestPrev != null) {
                previous.put(number, bestPrev);
            }
        }

        String tailFlight = longestPathLength.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (tailFlight == null) {
            return Optional.empty();
        }

        List<String> chain = new ArrayList<>();
        String cursor = tailFlight;
        while (cursor != null) {
            chain.add(cursor);
            cursor = previous.get(cursor);
        }
        Collections.reverse(chain);

        if (chain.size() < 2) {
            return Optional.empty();
        }

        ScheduledOperation startOp = schedule.get(chain.get(0));
        ScheduledOperation endOp = schedule.get(chain.get(chain.size() - 1));
        return Optional.of(new BottleneckChain(
                chain,
                longestPathLength.get(tailFlight),
                startOp.startSec(),
                endOp.endSec()));
    }

    private List<String> topologicalOrder(Map<String, Flight> active) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (Flight f : active.values()) {
            indegree.putIfAbsent(f.flightNumber(), 0);
            for (String dep : f.dependencies()) {
                if (!active.containsKey(dep)) {
                    continue;
                }
                indegree.merge(f.flightNumber(), 1, Integer::sum);
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(f.flightNumber());
            }
        }

        java.util.PriorityQueue<String> ready = new java.util.PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }
        List<String> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String next = ready.poll();
            ordered.add(next);
            for (String child : dependents.getOrDefault(next, List.of())) {
                int remaining = indegree.merge(child, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(child);
                }
            }
        }
        return ordered;
    }

    public record BottleneckChain(List<String> flightNumbers,
                                  long totalDurationSeconds,
                                  long startSec,
                                  long endSec) { }
}
