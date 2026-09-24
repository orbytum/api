package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.AtividadeStatus;

import java.time.LocalDateTime;

public record AtividadeResponse(
        Long id,
        Long projetoId,
        Long responsavelId,
        String responsavelNome,
        Long atividadePaiId,
        String titulo,
        String descricao,
        AtividadeStatus status,
        LocalDateTime dthRegistro,
        LocalDateTime dthPrazo,
        LocalDateTime dthConclusao,
        boolean isAtrasada,
        boolean isAtivo,
        boolean projetoPodeSerFinalizado
) {}
