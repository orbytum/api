package com.orbytum.api.models.dto.response;

public record RoleResponse(
        Long id,
        String nome,
        boolean isLider
) {}
