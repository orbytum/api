package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ConviteGrupoResponse(
        Long idConvite,
        String token,
        String urlConvite,
        Long idGrupo,
        String nomeGrupo,
        List<Long> idsProjeto,
        LocalDateTime dthRegistro,
        LocalDateTime dthExpiracao,
        boolean isAtivo,
        String cargo,
        Integer limiteUso,
        Integer usos
) {
    public ConviteGrupoResponse(
            Long idConvite,
            String token,
            String urlConvite,
            Long idGrupo,
            String nomeGrupo,
            List<Long> idsProjeto,
            LocalDateTime dthRegistro,
            LocalDateTime dthExpiracao,
            boolean isAtivo
    ) {
        this(idConvite, token, urlConvite, idGrupo, nomeGrupo, idsProjeto, dthRegistro, dthExpiracao, isAtivo, null, null, 0);
    }
}

