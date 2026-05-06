package com.events.platform.web.dto;

import com.events.platform.domain.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record InviteCreateRequest(@NotNull MembershipRole role) {}
