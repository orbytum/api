package com.orbytum.api.models.dto.response;

public record PesquisadorResponse(
        Long usuarioId,
        String nome,
        String email,
        String telefone,
        String titulo,
        Long grupoId,
        String cargo,
        boolean isLider
) {}
