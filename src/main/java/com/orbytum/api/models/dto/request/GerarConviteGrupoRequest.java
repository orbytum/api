package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.NivelMembro;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GerarConviteGrupoRequest(
        @NotNull(message = "O ID do grupo é obrigatório")
        Long idGrupo,
        List<Long> idsProjeto,
        Integer diasValidade,
        Integer limiteUso,
        Long idRole,
        NivelMembro nivel
) {
    public GerarConviteGrupoRequest(Long idGrupo, List<Long> idsProjeto, Integer diasValidade) {
        this(idGrupo, idsProjeto, diasValidade, null, null, null);
    }

    public GerarConviteGrupoRequest(Long idGrupo, List<Long> idsProjeto, Integer diasValidade, Integer limiteUso) {
        this(idGrupo, idsProjeto, diasValidade, limiteUso, null, null);
    }
}
