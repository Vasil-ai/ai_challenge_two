package com.events.platform.web.dto;

public record RsvpResponse(
        String status,
        String ticketCode,
        Integer waitlistPosition,
        boolean promotionPending,
        EventResponse event) {}
