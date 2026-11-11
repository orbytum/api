package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.NivelMembro;

public record ParticipanteResponse(
        Long usuarioId,
        String nome,
        String email,
        NivelMembro nivel,
        boolean isAtivo
) {}
