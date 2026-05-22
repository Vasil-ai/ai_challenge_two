package com.atc.mcp.scenarios;

import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.UnscheduledReason;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.service.ScheduleService.ScheduleSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationScenarioTest extends AbstractScenarioTest {

    @Test
    void cancellingDependencyMakesDependentUnscheduled() {
        toolService.submitFlight("INB", "ARRIVAL", "MEDIUM", null, null);
        toolService.submitFlight("OUT", "DEPARTURE", "HIGH", List.of("INB"), null);
        toolService.generateSchedule();

        toolService.cancelFlight("INB");
        ScheduleSummary summary = toolService.generateSchedule().data();

        FlightView outbound = summary.unscheduled().stream()
                .filter(f -> f.flightNumber().equals("OUT")).findFirst().orElseThrow();
        assertThat(outbound.status()).isEqualTo(FlightStatus.UNSCHEDULED);
        assertThat(outbound.unscheduledReason())
                .isIn(UnscheduledReason.DEPENDENCY_CANCELLED,
                        UnscheduledReason.DEPENDENCY_UNSCHEDULED);

        FlightView inbound = summary.cancelled().stream()
                .filter(f -> f.flightNumber().equals("INB")).findFirst().orElseThrow();
        assertThat(inbound.status()).isEqualTo(FlightStatus.CANCELLED);
    }
}
