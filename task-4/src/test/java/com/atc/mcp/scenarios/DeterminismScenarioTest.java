package com.atc.mcp.scenarios;

import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.service.ScheduleService.ScheduleSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterminismScenarioTest extends AbstractScenarioTest {

    @Test
    void schedulingTwiceWithSameInputsIsDeterministic() {
        seed();
        ScheduleSummary first = toolService.generateSchedule().data();
        ScheduleSummary second = toolService.generateSchedule().data();

        assertThat(extractRows(first.scheduled()))
                .containsExactlyElementsOf(extractRows(second.scheduled()));
    }

    private void seed() {
        toolService.submitFlight("BA10", "ARRIVAL", "HIGH", null, null);
        toolService.submitFlight("LH20", "DEPARTURE", "MEDIUM", null, null);
        toolService.submitFlight("AF30", "ARRIVAL", "LOW", null, null);
        toolService.submitFlight("KL40", "DEPARTURE", "LOW", null, null);
        toolService.submitFlight("OUT50", "DEPARTURE", "HIGH", List.of("BA10"), null);
    }

    private List<String> extractRows(List<FlightView> flights) {
        return flights.stream()
                .map(f -> String.join("|",
                        f.flightNumber(),
                        f.assignedRunwayId(),
                        f.assignedGateId(),
                        Long.toString(f.scheduledStartSec()),
                        Long.toString(f.scheduledEndSec())))
                .toList();
    }
}
