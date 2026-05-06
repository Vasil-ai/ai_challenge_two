package com.events.platform.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BecomeHostRequest(
        @NotBlank String displayName,
        @NotBlank String slug,
        String bio,
        @Email @NotBlank String contactEmail,
        String logoUrl) {}
