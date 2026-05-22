package com.atc.mcp.exception;

/**
 * Thrown when a flight number is submitted twice.
 */
public class DuplicateFlightException extends RuntimeException {

    public DuplicateFlightException(String flightNumber) {
        super("Flight '" + flightNumber + "' is already submitted");
    }
}
