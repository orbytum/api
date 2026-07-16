package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String senha
) {}