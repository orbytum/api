package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ConviteLinkResponse(
        Long idConvite,
        String token,
        String url,
        Long idGrupo,
        String nomeGrupo,
        List<Long> idsProjeto,
        LocalDateTime dthRegistro,
        LocalDateTime dthExpiracao,
        boolean isAtivo
) {}
