package com.atc.mcp.domain;

public enum OperationType {
    ARRIVAL,
    DEPARTURE;

    public boolean isArrival() {
        return this == ARRIVAL;
    }
}
