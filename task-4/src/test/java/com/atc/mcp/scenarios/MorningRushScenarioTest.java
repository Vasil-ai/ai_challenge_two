package com.atc.mcp.scenarios;

import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.mcp.dto.AirportStatusView;
import com.atc.mcp.mcp.dto.BottleneckView;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.mcp.tools.AtcToolService.ToolResponse;
import com.atc.mcp.service.ScheduleService.ScheduleSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MorningRushScenarioTest extends AbstractScenarioTest {

    @Test
    void allSchedulableFlightsScheduledWithoutOverlapAndPriorityRespected() {
        toolService.submitFlight("BA101", "ARRIVAL", "HIGH", null, null);
        toolService.submitFlight("LH202", "DEPARTURE", "MEDIUM", null, null);
        toolService.submitFlight("AF303", "ARRIVAL", "LOW", null, null);
        toolService.submitFlight("KL404", "DEPARTURE", "LOW", null, null);

        ToolResponse<ScheduleSummary> resp = toolService.generateSchedule();
        assertThat(resp.ok()).isTrue();

        ScheduleSummary summary = resp.data();
        assertThat(summary.scheduled()).hasSize(4);
        assertThat(summary.unscheduled()).isEmpty();

        FlightView highPriority = summary.scheduled().stream()
                .filter(f -> f.flightNumber().equals("BA101")).findFirst().orElseThrow();
        FlightView lowPriorityArrival = summary.scheduled().stream()
                .filter(f -> f.flightNumber().equals("AF303")).findFirst().orElseThrow();
        assertThat(highPriority.scheduledStartSec())
                .isLessThanOrEqualTo(lowPriorityArrival.scheduledStartSec());

        AirportStatusView status = toolService.getAirportStatus().data();
        assertThat(status.flightCountsByStatus().get(FlightStatus.SCHEDULED.name())).isEqualTo(4);
        assertThat(status.unscheduledFlights()).isEmpty();
        assertThat(status.scheduleCompletionSec()).isPositive();

        BottleneckView bottleneck = toolService.analyzeBottleneck().data();
        assertThat(bottleneck.exists()).isFalse();

        assertNoRunwayOverlap(summary.scheduled());
    }

    private void assertNoRunwayOverlap(List<FlightView> flights) {
        for (int i = 0; i < flights.size(); i++) {
            for (int j = i + 1; j < flights.size(); j++) {
                FlightView a = flights.get(i);
                FlightView b = flights.get(j);
                if (!a.assignedRunwayId().equals(b.assignedRunwayId())) {
                    continue;
                }
                boolean overlaps = a.scheduledStartSec() < b.scheduledEndSec()
                        && b.scheduledStartSec() < a.scheduledEndSec();
                assertThat(overlaps)
                        .as("Runway " + a.assignedRunwayId() + " overlap between "
                                + a.flightNumber() + " and " + b.flightNumber())
                        .isFalse();
            }
        }
    }
}
