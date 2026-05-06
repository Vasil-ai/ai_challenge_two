package com.events.platform.web.dto;

import com.events.platform.domain.ReportTargetType;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(@NotNull ReportTargetType targetType, @NotNull Long targetId) {}
