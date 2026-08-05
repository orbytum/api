package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;

public record ConviteEnviadoResponse(
        Long id,
        Long grupoId,
        String nomeGrupo,
        String emailConvidado,
        LocalDateTime dthRegistro,
        LocalDateTime dthExpiracao,
        boolean isAtivo
) {}
