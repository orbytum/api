package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
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
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        @NotNull(message = "A data de entrega é obrigatória")
        LocalDateTime dthPrazo
) {}
