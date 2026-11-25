package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record EditarSolicitacaoRequest(
    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
    String titulo,

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
    String descricao,

    @NotBlank(message = "A justificativa é obrigatória.")
    String justificativa,

    Integer quantidade,
    BigDecimal valor
) {}
