package com.atc.mcp.exception;

/**
 * Thrown during application startup when the airport configuration is invalid.
 * Causes the Spring {@code ApplicationContext} to fail fast with exit code 1.
 */
public class InvalidConfigurationException extends RuntimeException {

    public InvalidConfigurationException(String message) {
        super(message);
    }
}
