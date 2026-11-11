package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.AtividadeStatus;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusAtividadeRequest(
        @NotNull(message = "O status é obrigatório")
        AtividadeStatus status
) {}
