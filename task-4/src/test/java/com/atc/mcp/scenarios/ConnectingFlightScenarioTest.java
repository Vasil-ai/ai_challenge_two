package com.atc.mcp.scenarios;

import com.atc.mcp.config.AirportProperties;
import com.atc.mcp.mcp.dto.BottleneckView;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.mcp.dto.TimelineEntry;
import com.atc.mcp.mcp.tools.AtcToolService.ToolResponse;
import com.atc.mcp.service.ScheduleService.ScheduleSummary;
import com.atc.mcp.service.StatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectingFlightScenarioTest extends AbstractScenarioTest {

    @Autowired
    private AirportProperties properties;

    @Autowired
    private StatusService statusService;

    @Test
    void outboundStartsAfterInboundPlusDependencyBuffer() {
        toolService.submitFlight("INB1", "ARRIVAL", "MEDIUM", null, null);
        toolService.submitFlight("OUT1", "DEPARTURE", "HIGH", List.of("INB1"), null);

        ToolResponse<ScheduleSummary> resp = toolService.generateSchedule();
        assertThat(resp.ok()).isTrue();

        ScheduleSummary summary = resp.data();
        assertThat(summary.scheduled()).hasSize(2);

        FlightView inbound = summary.scheduled().stream()
                .filter(f -> f.flightNumber().equals("INB1")).findFirst().orElseThrow();
        FlightView outbound = summary.scheduled().stream()
                .filter(f -> f.flightNumber().equals("OUT1")).findFirst().orElseThrow();

        assertThat(outbound.scheduledStartSec())
                .isGreaterThanOrEqualTo(
                        inbound.scheduledEndSec() + properties.dependencyBufferSeconds());

        List<TimelineEntry> timeline = statusService.buildTimeline();
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).flightNumber()).isEqualTo("INB1");
        assertThat(timeline.get(1).flightNumber()).isEqualTo("OUT1");

        BottleneckView bottleneck = toolService.analyzeBottleneck().data();
        assertThat(bottleneck.exists()).isTrue();
        assertThat(bottleneck.flightNumbers()).containsExactly("INB1", "OUT1");
    }
}
