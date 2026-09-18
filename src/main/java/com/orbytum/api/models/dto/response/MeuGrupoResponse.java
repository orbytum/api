package com.orbytum.api.models.dto.response;

public record MeuGrupoResponse(
        Long id,
        String nome,
        String role,
        boolean isLider
) {
}
