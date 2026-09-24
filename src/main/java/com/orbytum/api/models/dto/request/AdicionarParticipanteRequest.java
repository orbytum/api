package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.NivelMembro;
import jakarta.validation.constraints.NotNull;

public record AdicionarParticipanteRequest(
        @NotNull(message = "O ID do usuário é obrigatório")
        Long usuarioId,

        NivelMembro nivel
) {}
