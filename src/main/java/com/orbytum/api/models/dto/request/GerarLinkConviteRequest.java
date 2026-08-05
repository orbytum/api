package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GerarLinkConviteRequest(
        @NotNull(message = "O ID do grupo é obrigatório")
        Long idGrupo,
        List<Long> idsProjeto,
        Integer diasValidade,
        Integer limiteUso
) {}
