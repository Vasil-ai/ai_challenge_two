package com.atc.mcp;

import com.atc.mcp.config.AirportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AirportProperties.class)
public class AtcMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtcMcpApplication.class, args);
    }
}
