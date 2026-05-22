package com.atc.mcp.mcp.tools;

import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.exception.DuplicateFlightException;
import com.atc.mcp.exception.FlightNotFoundException;
import com.atc.mcp.mcp.dto.AirportStatusView;
import com.atc.mcp.mcp.dto.BottleneckView;
import com.atc.mcp.mcp.dto.FlightView;
import com.atc.mcp.service.FlightService;
import com.atc.mcp.service.ScheduleService;
import com.atc.mcp.service.ScheduleService.ScheduleSummary;
import com.atc.mcp.service.StatusService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * MCP tools that AI clients can invoke to coordinate the airport. Each method
 * is registered automatically by {@code MethodToolCallbackProvider} in
 * {@link com.atc.mcp.config.McpServerConfig}.
 */
@Component
public class AtcToolService {

    private final FlightService flightService;
    private final ScheduleService scheduleService;
    private final StatusService statusService;

    public AtcToolService(FlightService flightService,
                          ScheduleService scheduleService,
                          StatusService statusService) {
        this.flightService = flightService;
        this.scheduleService = scheduleService;
        this.statusService = statusService;
    }

    @Tool(name = "submit_flight",
            description = "Submit a new arrival or departure to the airport flight queue. "
                    + "The flight is added in PENDING state and will be considered the next time "
                    + "generate_schedule is invoked.")
    public ToolResponse<FlightView> submitFlight(
            @ToolParam(description = "Unique flight number, e.g. 'BA123'.") String flightNumber,
            @ToolParam(description = "ARRIVAL or DEPARTURE.") String operationType,
            @ToolParam(description = "HIGH, MEDIUM or LOW.") String priority,
            @ToolParam(description = "Optional list of flight numbers this flight depends on.",
                    required = false) List<String> dependencies,
            @ToolParam(description = "Optional minimum runway length in meters.",
                    required = false) Integer minRunwayLengthMeters) {
        try {
            OperationType op = OperationType.valueOf(operationType.trim().toUpperCase(Locale.ROOT));
            Priority pr = Priority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
            FlightView view = flightService.submit(
                    flightNumber, op, pr, dependencies, minRunwayLengthMeters);
            return ToolResponse.ok("flight_submitted", view);
        } catch (IllegalArgumentException ex) {
            return ToolResponse.failure("invalid_arguments", ex.getMessage());
        } catch (DuplicateFlightException ex) {
            return ToolResponse.failure("duplicate_flight", ex.getMessage());
        }
    }

    @Tool(name = "cancel_flight",
            description = "Cancel a previously submitted flight. Cancelled flights remain "
                    + "visible in the queue. Dependent flights are marked as needing reevaluation "
                    + "and will be re-considered the next time generate_schedule is invoked.")
    public ToolResponse<FlightView> cancelFlight(
            @ToolParam(description = "Flight number to cancel.") String flightNumber) {
        try {
            FlightView view = flightService.cancel(flightNumber);
            return ToolResponse.ok("flight_cancelled", view);
        } catch (FlightNotFoundException ex) {
            return ToolResponse.failure("flight_not_found", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ToolResponse.failure("invalid_arguments", ex.getMessage());
        }
    }

    @Tool(name = "generate_schedule",
            description = "Recompute the airport schedule from the current flight queue and "
                    + "configuration. Replaces the existing schedule with a freshly computed one. "
                    + "Repeated calls with the same inputs are deterministic.")
    public ToolResponse<ScheduleSummary> generateSchedule() {
        ScheduleSummary summary = scheduleService.regenerate();
        return ToolResponse.ok("schedule_generated", summary);
    }

    @Tool(name = "get_airport_status",
            description = "Return a structured operational status: flight counts by state and "
                    + "operation type, runway and gate capacity/usage, resource constraint "
                    + "indicators, unscheduled flights with reasons, and the schedule "
                    + "completion time when available.")
    public ToolResponse<AirportStatusView> getAirportStatus() {
        return ToolResponse.ok("ok", statusService.buildStatus());
    }

    @Tool(name = "analyze_bottleneck",
            description = "Identify the longest active scheduled dependency chain. The result "
                    + "includes the ordered flights and the total elapsed duration accounting "
                    + "for operation durations and required dependency buffers.")
    public ToolResponse<BottleneckView> analyzeBottleneck() {
        BottleneckView view = statusService.analyzeBottleneck();
        return ToolResponse.ok(view.exists() ? "ok" : "no_chain", view);
    }

    /**
     * Common response envelope used by all tools.
     */
    public record ToolResponse<T>(boolean ok, String code, String message, T data) {

        public static <T> ToolResponse<T> ok(String code, T data) {
            return new ToolResponse<>(true, code, null, data);
        }

        public static <T> ToolResponse<T> failure(String code, String message) {
            return new ToolResponse<>(false, code, message, null);
        }
    }
}
