package com.events.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteAcceptRequest(@NotBlank String token, @NotNull Long hostId) {}
