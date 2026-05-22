package com.atc.mcp.config;

import com.atc.mcp.config.AirportProperties.RunwaySpec;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses comma-separated runway specs from environment variables.
 * <p>Format: {@code id1:lengthMeters,id2:lengthMeters,...}, e.g.
 * {@code R1:3500,R2:2500,R3:4000}.</p>
 */
@Component
@ConfigurationPropertiesBinding
public class RunwaySpecListConverter implements Converter<String, List<RunwaySpec>> {

    @Override
    public List<RunwaySpec> convert(String source) {
        if (!StringUtils.hasText(source)) {
            return List.of();
        }
        String[] tokens = source.split(",");
        List<RunwaySpec> specs = new ArrayList<>(tokens.length);
        for (String raw : tokens) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon <= 0 || colon == token.length() - 1) {
                throw new IllegalArgumentException(
                        "Invalid runway spec '" + token + "'. Expected format 'id:lengthMeters'");
            }
            String id = token.substring(0, colon).trim();
            String lengthLiteral = token.substring(colon + 1).trim();
            int length;
            try {
                length = Integer.parseInt(lengthLiteral);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Runway '" + id + "' has non-integer length '" + lengthLiteral + "'");
            }
            specs.add(new RunwaySpec(id, length));
        }
        return List.copyOf(specs);
    }
}
