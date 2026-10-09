package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.MaterialStatus;

import java.time.LocalDateTime;

public record MaterialResponse(
        Long id,
        String nome,
        String descricao,
        String identificador,
        Integer quantidade,
        String localizacao,
        MaterialStatus status,
        LocalDateTime dthCompra,
        LocalDateTime dthRegistro,
        boolean isAtivo,
        Integer quantidadeEmprestada,
        Integer quantidadeDisponivel,
        String usuarioEmUsoNome,
        LocalDateTime dthDevolucao
) {}
