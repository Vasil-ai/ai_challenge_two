package com.atc.mcp.scheduler;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.domain.RunwayRequirements;
import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.domain.UnscheduledReason;
import com.atc.mcp.support.AirportPropertiesFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingEngineTest {

    private long counter;

    @Test
    void schedulesAllFlightsWithoutOverlapAndPrioritisesHigh() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        SchedulingEngine engine = new SchedulingEngine(config);

        Flight high = flight("HIGH1", OperationType.ARRIVAL, Priority.HIGH);
        Flight medium = flight("MED1", OperationType.DEPARTURE, Priority.MEDIUM);
        Flight low1 = flight("LOW1", OperationType.ARRIVAL, Priority.LOW);
        Flight low2 = flight("LOW2", OperationType.DEPARTURE, Priority.LOW);

        ScheduleResult result = engine.schedule(List.of(high, medium, low1, low2), Set.of());

        assertThat(result.scheduled()).hasSize(4);
        assertThat(result.unscheduled()).isEmpty();

        ScheduledOperation highOp = result.scheduled().get("HIGH1");
        ScheduledOperation lowOp = result.scheduled().get("LOW1");
        assertThat(highOp.startSec()).isLessThanOrEqualTo(lowOp.startSec());

        assertNoRunwayOverlap(result.scheduled().values(), config);
        assertNoGateOverlap(result.scheduled().values(), config);
    }

    @Test
    void rejectsFlightWithRunwayLengthExceedingAllRunways() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        SchedulingEngine engine = new SchedulingEngine(config);

        Flight oversized = new Flight("HEAVY1", OperationType.DEPARTURE, Priority.HIGH,
                List.of(), new RunwayRequirements(5_000), counter++);
        Flight normal = flight("OK1", OperationType.DEPARTURE, Priority.MEDIUM);

        ScheduleResult result = engine.schedule(List.of(oversized, normal), Set.of());

        assertThat(result.scheduled()).containsOnlyKeys("OK1");
        assertThat(result.unscheduled()).containsKey("HEAVY1");
        assertThat(result.unscheduled().get("HEAVY1").reason())
                .isEqualTo(UnscheduledReason.NO_SUITABLE_RUNWAY);
    }

    @Test
    void respectsDependencyAndDependencyBuffer() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        SchedulingEngine engine = new SchedulingEngine(config);

        Flight inbound = flight("IN1", OperationType.ARRIVAL, Priority.MEDIUM);
        Flight outbound = new Flight("OUT1", OperationType.DEPARTURE, Priority.HIGH,
                List.of("IN1"), RunwayRequirements.NONE, counter++);

        ScheduleResult result = engine.schedule(List.of(inbound, outbound), Set.of());

        ScheduledOperation in = result.scheduled().get("IN1");
        ScheduledOperation out = result.scheduled().get("OUT1");
        assertThat(in).isNotNull();
        assertThat(out).isNotNull();
        assertThat(out.startSec()).isGreaterThanOrEqualTo(
                in.endSec() + config.dependencyBufferSeconds());
    }

    @Test
    void respectsRunwaySeparationBufferBetweenSameType() {
        AirportProperties config = AirportPropertiesFactory.singleRunway();
        SchedulingEngine engine = new SchedulingEngine(config);

        Flight a = flight("A", OperationType.ARRIVAL, Priority.HIGH);
        Flight b = flight("B", OperationType.ARRIVAL, Priority.HIGH);

        ScheduleResult result = engine.schedule(List.of(a, b), Set.of());

        ScheduledOperation aOp = result.scheduled().get("A");
        ScheduledOperation bOp = result.scheduled().get("B");
        assertThat(bOp.startSec()).isGreaterThanOrEqualTo(
                aOp.endSec() + config.separation().landingSeconds());
    }

    @Test
    void deterministicAcrossRepeatedRuns() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        SchedulingEngine engine = new SchedulingEngine(config);

        List<Flight> first = sampleFlightSet();
        List<Flight> second = sampleFlightSet();

        ScheduleResult firstResult = engine.schedule(first, Set.of());
        ScheduleResult secondResult = engine.schedule(second, Set.of());

        assertThat(firstResult.scheduled()).containsExactlyEntriesOf(secondResult.scheduled());
    }

    @Test
    void picksSmallestFittingRunwayDeterministically() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        SchedulingEngine engine = new SchedulingEngine(config);

        Flight standard = flight("S1", OperationType.DEPARTURE, Priority.HIGH);

        ScheduleResult result = engine.schedule(List.of(standard), Set.of());

        ScheduledOperation op = result.scheduled().get("S1");
        assertThat(op.runwayId()).isEqualTo("R1");
    }

    private List<Flight> sampleFlightSet() {
        counter = 0;
        return List.of(
                flight("ALPHA", OperationType.ARRIVAL, Priority.HIGH),
                flight("BRAVO", OperationType.DEPARTURE, Priority.MEDIUM),
                flight("CHARLIE", OperationType.ARRIVAL, Priority.LOW),
                flight("DELTA", OperationType.DEPARTURE, Priority.LOW));
    }

    private Flight flight(String number, OperationType type, Priority priority) {
        return new Flight(number, type, priority, List.of(),
                RunwayRequirements.NONE, counter++);
    }

    private void assertNoRunwayOverlap(Iterable<ScheduledOperation> ops, AirportProperties config) {
        List<ScheduledOperation> list = new ArrayList<>();
        ops.forEach(list::add);
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                ScheduledOperation a = list.get(i);
                ScheduledOperation b = list.get(j);
                if (!a.runwayId().equals(b.runwayId())) {
                    continue;
                }
                long sep = config.separation().forTransition(
                        a.operationType().isArrival(), b.operationType().isArrival());
                if (a.startSec() <= b.startSec()) {
                    assertThat(b.startSec()).isGreaterThanOrEqualTo(a.endSec() + sep);
                } else {
                    assertThat(a.startSec()).isGreaterThanOrEqualTo(b.endSec() + sep);
                }
            }
        }
    }

    private void assertNoGateOverlap(Iterable<ScheduledOperation> ops, AirportProperties config) {
        List<ScheduledOperation> list = new ArrayList<>();
        ops.forEach(list::add);
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                ScheduledOperation a = list.get(i);
                ScheduledOperation b = list.get(j);
                if (!a.gateId().equals(b.gateId())) {
                    continue;
                }
                long aBusyEnd = a.endSec() + config.gateTurnaroundSeconds();
                long bBusyEnd = b.endSec() + config.gateTurnaroundSeconds();
                boolean overlaps = a.startSec() < bBusyEnd && b.startSec() < aBusyEnd;
                assertThat(overlaps).isFalse();
            }
        }
    }
}
