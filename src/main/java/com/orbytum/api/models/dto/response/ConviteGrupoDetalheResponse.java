package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.NivelMembro;

import java.time.LocalDateTime;

public record ConviteGrupoDetalheResponse(
        Long idConvite,
        String token,
        Long idGrupo,
        String nomeGrupo,
        String nomeRemetente,
        String cargo,
        NivelMembro nivel,
        LocalDateTime dthExpiracao,
        boolean isAtivo
) {}