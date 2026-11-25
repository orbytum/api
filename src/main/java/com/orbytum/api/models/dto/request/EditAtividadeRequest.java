package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record EditAtividadeRequest(
        Long responsavelId,

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        LocalDateTime dthPrazo
) {}
