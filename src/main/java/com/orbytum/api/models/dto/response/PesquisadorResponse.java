package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.NivelMembro;

public record PesquisadorResponse(
        Long usuarioId,
        String nome,
        String email,
        String telefone,
        String titulo,
        Long grupoId,
        String cargo,
        NivelMembro nivel,
        String funcao,
        boolean isLider
) {}
