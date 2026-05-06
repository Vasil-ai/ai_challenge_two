package com.events.platform.web.dto;

public record HostResponse(
        Long id, String slug, String displayName, String logoUrl, String bio, String contactEmail) {}
