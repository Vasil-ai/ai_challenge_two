package com.events.platform.web.dto;

import java.util.List;

public record HostPublicResponse(HostResponse host, List<EventResponse> publishedEvents) {}
