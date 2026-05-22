package com.atc.mcp.scenarios;

import com.atc.mcp.domain.FlightStatus;
import com.atc.mcp.domain.UnscheduledReason;
import com.atc.mcp.mcp.dto.AirportStatusView;
import com.atc.mcp.mcp.dto.AirportStatusView.UnscheduledFlightView;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.mcp.tools.AtcToolService.ToolResponse;
import com.atc.mcp.service.ScheduleService.ScheduleSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeavyHaulerScenarioTest extends AbstractScenarioTest {

    @Test
    void oversizedFlightRemainsUnscheduledOthersScheduled() {
        toolService.submitFlight("HEAVY1", "DEPARTURE", "HIGH", null, 6000);
        toolService.submitFlight("FAST1", "DEPARTURE", "MEDIUM", null, null);

        ToolResponse<ScheduleSummary> resp = toolService.generateSchedule();
        assertThat(resp.ok()).isTrue();

        ScheduleSummary summary = resp.data();
        assertThat(summary.scheduled())
                .extracting(FlightView::flightNumber)
                .containsExactly("FAST1");
        assertThat(summary.unscheduled())
                .extracting(FlightView::flightNumber, FlightView::status, FlightView::unscheduledReason)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "HEAVY1",
                                FlightStatus.UNSCHEDULED,
                                UnscheduledReason.NO_SUITABLE_RUNWAY));

        AirportStatusView status = toolService.getAirportStatus().data();
        assertThat(status.unscheduledFlights())
                .extracting(UnscheduledFlightView::flightNumber, UnscheduledFlightView::reason)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "HEAVY1", UnscheduledReason.NO_SUITABLE_RUNWAY));
    }
}
