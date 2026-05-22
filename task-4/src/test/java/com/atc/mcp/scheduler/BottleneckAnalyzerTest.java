package com.atc.mcp.scheduler;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.domain.RunwayRequirements;
import com.atc.mcp.domain.ScheduledOperation;
import com.atc.mcp.support.AirportPropertiesFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BottleneckAnalyzerTest {

    @Test
    void returnsLongestChainAcrossDependencies() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        BottleneckAnalyzer analyzer = new BottleneckAnalyzer(config);

        Flight a = flight("A");
        Flight b = depFlight("B", "A");
        Flight c = depFlight("C", "B");
        Flight d = flight("D");

        a.markScheduled("R1", "G01", 0, 600);
        b.markScheduled("R1", "G02", 1500, 1980);
        c.markScheduled("R1", "G01", 2900, 3380);
        d.markScheduled("R2", "G02", 0, 480);

        Map<String, ScheduledOperation> schedule = new LinkedHashMap<>();
        for (Flight f : List.of(a, b, c, d)) {
            schedule.put(f.flightNumber(), new ScheduledOperation(
                    f.flightNumber(), f.operationType(),
                    f.assignedRunwayId(), f.assignedGateId(),
                    f.scheduledStartSec(), f.scheduledEndSec()));
        }

        Optional<BottleneckAnalyzer.BottleneckChain> chain =
                analyzer.analyze(List.of(a, b, c, d), schedule);

        assertThat(chain).isPresent();
        assertThat(chain.get().flightNumbers()).containsExactly("A", "B", "C");
        long expected = 600 + 480 + 480 + 2 * config.dependencyBufferSeconds();
        assertThat(chain.get().totalDurationSeconds()).isEqualTo(expected);
    }

    @Test
    void emptyWhenNoChainOfLengthTwo() {
        AirportProperties config = AirportPropertiesFactory.defaults();
        BottleneckAnalyzer analyzer = new BottleneckAnalyzer(config);

        Flight onlyOne = flight("ONLY");
        onlyOne.markScheduled("R1", "G01", 0, 600);
        Map<String, ScheduledOperation> schedule = Map.of(
                onlyOne.flightNumber(),
                new ScheduledOperation(onlyOne.flightNumber(), onlyOne.operationType(),
                        "R1", "G01", 0, 600));

        Optional<BottleneckAnalyzer.BottleneckChain> chain =
                analyzer.analyze(List.of(onlyOne), schedule);

        assertThat(chain).isEmpty();
    }

    private static Flight flight(String number) {
        return new Flight(number, OperationType.DEPARTURE, Priority.MEDIUM, List.of(),
                RunwayRequirements.NONE, number.hashCode());
    }

    private static Flight depFlight(String number, String dep) {
        return new Flight(number, OperationType.DEPARTURE, Priority.MEDIUM, List.of(dep),
                RunwayRequirements.NONE, number.hashCode());
    }
}
