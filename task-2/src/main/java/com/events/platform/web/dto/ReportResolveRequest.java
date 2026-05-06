package com.events.platform.web.dto;

import jakarta.validation.constraints.NotNull;

public record ReportResolveRequest(@NotNull Boolean hide) {}
