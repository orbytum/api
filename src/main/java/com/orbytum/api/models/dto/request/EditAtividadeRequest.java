package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record EditAtividadeRequest(
        Long responsavelId,

        @NotBlank(message = "O título é obrigatório")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        LocalDateTime dthPrazo
) {}
