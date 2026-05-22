package com.atc.mcp.mcp.resources;

import com.atc.mcp.service.StatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the three MCP resources required by the spec:
 * <ul>
 *   <li>{@code atc://flights/queue} — current flight queue grouped by status.</li>
 *   <li>{@code atc://runways} — runway capacity and current usage.</li>
 *   <li>{@code atc://timeline} — chronological schedule of operations.</li>
 * </ul>
 * Each resource is rendered to JSON using the application {@link ObjectMapper}
 * so MCP clients can parse the contents directly.
 */
@Configuration
public class AtcResources {

    private static final String JSON_MIME = "application/json";

    @Bean
    public List<SyncResourceSpecification> atcResourceList(StatusService statusService,
                                                            ObjectMapper objectMapper) {
        return List.of(
                buildResource(
                        "atc://flights/queue",
                        "Flight Queue",
                        "All flights grouped by status: scheduled, unscheduled, pending, cancelled.",
                        objectMapper,
                        statusService::buildQueue),
                buildResource(
                        "atc://runways",
                        "Runways",
                        "Runway list, capability, current usage windows and per-runway operation counts.",
                        objectMapper,
                        statusService::buildRunwayUsage),
                buildResource(
                        "atc://timeline",
                        "Timeline",
                        "Chronological list of scheduled operations with absolute timestamps.",
                        objectMapper,
                        statusService::buildTimeline)
        );
    }

    private SyncResourceSpecification buildResource(String uri,
                                                    String name,
                                                    String description,
                                                    ObjectMapper objectMapper,
                                                    Supplier<?> payloadSupplier) {
        Resource resource = new McpSchema.Resource(uri, name, description, JSON_MIME, null);
        return new McpServerFeatures.SyncResourceSpecification(resource,
                (exchange, request) -> renderResource(uri, objectMapper, payloadSupplier));
    }

    private ReadResourceResult renderResource(String uri,
                                              ObjectMapper objectMapper,
                                              Supplier<?> payloadSupplier) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(payloadSupplier.get());
            return new ReadResourceResult(List.of(
                    new TextResourceContents(uri, JSON_MIME, json)));
        } catch (JsonProcessingException ex) {
            String error = "{\"error\":\"failed_to_serialize_resource\",\"message\":\""
                    + escape(ex.getMessage()) + "\"}";
            return new ReadResourceResult(List.of(
                    new TextResourceContents(uri, JSON_MIME, error)));
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
