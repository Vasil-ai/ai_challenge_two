package com.atc.mcp.config;

import com.atc.mcp.mcp.tools.AtcToolService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the ATC tool service into Spring AI's MCP auto-configuration and
 * configures Jackson for ISO-8601 timestamps used in DTO projections.
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider atcTools(AtcToolService toolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(toolService)
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer atcJacksonCustomizer() {
        return builder -> builder
                .modulesToInstall(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
