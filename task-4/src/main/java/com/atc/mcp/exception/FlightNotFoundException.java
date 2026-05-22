package com.atc.mcp.exception;

/**
 * Thrown when a tool references a flight number that is not present in the
 * airport state.
 */
public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(String flightNumber) {
        super("Flight '" + flightNumber + "' is not known");
    }
}
