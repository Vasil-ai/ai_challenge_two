package com.events.platform.web.dto;

public record TicketRowResponse(EventResponse event, String ticketCode, boolean promotionPending) {}
