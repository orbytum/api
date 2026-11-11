package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAtividadeRequest(
        @NotNull(message = "O projeto é obrigatório")
        Long projetoId,

        @NotNull(message = "O responsável é obrigatório")
        Long responsavelId,

        Long atividadePaiId,

        @NotBlank(message = "O título é obrigatório")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotNull(message = "A data de entrega é obrigatória")
        LocalDateTime dthPrazo
) {}
