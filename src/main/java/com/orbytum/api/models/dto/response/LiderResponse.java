package com.orbytum.api.models.dto.response;

public record LiderResponse(
        Long usuarioId,
        String nome,
        String email,
        String telefone,
        String titulo,
        Long grupoId,
        boolean isLider) {
}