package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record EditarSolicitacaoRequest(
    @NotBlank(message = "O título é obrigatório.")
    String titulo,

    @NotBlank(message = "A descrição é obrigatória.")
    String descricao,

    @NotBlank(message = "A justificativa é obrigatória.")
    String justificativa,

    Integer quantidade,
    BigDecimal valor
) {}
