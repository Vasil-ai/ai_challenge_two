package com.atc.mcp.store;

import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.domain.RunwayRequirements;
import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.domain.UnscheduledReason;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Thread-safe in-memory aggregate root for the airport state. All writes happen
 * under a single write lock so the scheduler can recompute atomically. Snapshots
 * exposed to the outside world are deep copies of the internal flight records.
 */
@Component
public class AirportStateStore {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, Flight> flights = new LinkedHashMap<>();
    private final Map<String, ScheduledOperation> schedule = new LinkedHashMap<>();
    private final AtomicLong creationCounter = new AtomicLong();

    public boolean addFlight(Flight flight) {
        lock.writeLock().lock();
        try {
            if (flights.containsKey(flight.flightNumber())) {
                return false;
            }
            flights.put(flight.flightNumber(), flight);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Flight newFlight(String flightNumber,
                            OperationType operationType,
                            Priority priority,
                            List<String> dependencies,
                            RunwayRequirements runwayRequirements) {
        return new Flight(flightNumber, operationType, priority, dependencies, runwayRequirements,
                creationCounter.getAndIncrement());
    }

    public Optional<Flight> findFlight(String flightNumber) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(flights.get(flightNumber));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean cancel(String flightNumber) {
        lock.writeLock().lock();
        try {
            Flight flight = flights.get(flightNumber);
            if (flight == null || flight.status() == FlightStatus.CANCELLED) {
                return false;
            }
            flight.markCancelled();
            schedule.remove(flightNumber);
            for (Flight other : flights.values()) {
                if (other.dependencies().contains(flightNumber)
                        && other.status() != FlightStatus.CANCELLED) {
                    other.markUnscheduled(UnscheduledReason.DEPENDENCY_CANCELLED,
                            "Dependency '" + flightNumber + "' was cancelled");
                }
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Replaces flight states and schedule atomically. Used by the scheduler
     * after computing a fresh schedule.
     */
    public <R> R applyScheduleResult(Function<Map<String, Flight>, ScheduleApplication<R>> action) {
        lock.writeLock().lock();
        try {
            ScheduleApplication<R> apply = action.apply(flights);
            schedule.clear();
            schedule.putAll(apply.schedule());
            return apply.value();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <R> R readWith(Function<ReadView, R> reader) {
        lock.readLock().lock();
        try {
            return reader.apply(new ReadView(flights, schedule));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void readVoid(Consumer<ReadView> reader) {
        lock.readLock().lock();
        try {
            reader.accept(new ReadView(flights, schedule));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            flights.clear();
            schedule.clear();
            creationCounter.set(0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * View over the live store; only valid while the owning lock is held.
     */
    public record ReadView(Map<String, Flight> flights, Map<String, ScheduledOperation> schedule) {

        public List<Flight> snapshotFlights() {
            return new ArrayList<>(flights.values());
        }

        public List<ScheduledOperation> snapshotSchedule() {
            return new ArrayList<>(schedule.values());
        }
    }

    /**
     * Result of a scheduling action returned to the store under the write lock.
     */
    public record ScheduleApplication<R>(Map<String, ScheduledOperation> schedule, R value) { }
}
