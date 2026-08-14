package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ConviteGrupoEnviadoResponse(
        Long id,
        Long idGrupo,
        String nomeGrupo,
        String emailConvidado,
        List<Long> projetoIds,
        LocalDateTime dthRegistro,
        LocalDateTime dthExpiracao,
        boolean isAtivo
) {}
