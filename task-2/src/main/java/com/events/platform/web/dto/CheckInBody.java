package com.events.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInBody(@NotBlank String code) {}
