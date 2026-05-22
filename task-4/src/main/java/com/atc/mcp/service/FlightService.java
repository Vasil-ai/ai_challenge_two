package com.atc.mcp.service;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.domain.RunwayRequirements;
import com.atc.mcp.exception.DuplicateFlightException;
import com.atc.mcp.exception.FlightNotFoundException;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.store.AirportStateStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Coordinates submitting and cancelling flights. The service trusts that all
 * inputs have already been validated by the MCP layer; it focuses on store
 * coordination and dependency invalidation rules.
 */
@Service
public class FlightService {

    private final AirportStateStore store;
    private final AirportProperties properties;

    public FlightService(AirportStateStore store, AirportProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    public FlightView submit(String flightNumber,
                             OperationType operationType,
                             Priority priority,
                             List<String> dependencies,
                             Integer minRunwayLengthMeters) {
        String normalized = normalize(flightNumber);
        List<String> normalizedDeps = normalizeDependencies(dependencies, normalized);

        RunwayRequirements requirements = minRunwayLengthMeters == null
                ? RunwayRequirements.NONE
                : new RunwayRequirements(minRunwayLengthMeters);

        Flight flight = store.newFlight(normalized, operationType, priority, normalizedDeps, requirements);
        boolean added = store.addFlight(flight);
        if (!added) {
            throw new DuplicateFlightException(normalized);
        }
        return FlightView.from(flight, properties.epoch());
    }

    public FlightView cancel(String flightNumber) {
        String normalized = normalize(flightNumber);
        boolean cancelled = store.cancel(normalized);
        if (!cancelled) {
            throw new FlightNotFoundException(normalized);
        }
        return store.findFlight(normalized)
                .map(f -> FlightView.from(f, properties.epoch()))
                .orElseThrow(() -> new FlightNotFoundException(normalized));
    }

    private String normalize(String flightNumber) {
        if (flightNumber == null) {
            throw new IllegalArgumentException("flightNumber must not be null");
        }
        String trimmed = flightNumber.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("flightNumber must not be blank");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeDependencies(List<String> dependencies, String selfNumber) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(dependencies.size());
        for (String dep : dependencies) {
            if (dep == null) {
                continue;
            }
            String normalized = dep.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.equals(selfNumber)) {
                throw new IllegalArgumentException(
                        "Flight '" + selfNumber + "' cannot depend on itself");
            }
            if (!result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }
}
