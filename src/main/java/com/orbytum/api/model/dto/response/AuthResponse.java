package com.orbytum.api.model.dto.response;

public record AuthResponse(
        String token,
        String type
) {}
