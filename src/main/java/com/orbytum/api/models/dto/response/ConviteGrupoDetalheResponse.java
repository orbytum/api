package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;

public record ConviteGrupoDetalheResponse(
        Long idConvite,
        String token,
        Long idGrupo,
        String nomeGrupo,
        String nomeRemetente,
        String cargo,
        LocalDateTime dthExpiracao,
        boolean isAtivo
) {}