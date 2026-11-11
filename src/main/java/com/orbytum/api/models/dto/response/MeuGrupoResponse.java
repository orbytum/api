package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.NivelMembro;

public record MeuGrupoResponse(
        Long id,
        String nome,
        String role,
        NivelMembro nivel,
        boolean isLider
) {
}
