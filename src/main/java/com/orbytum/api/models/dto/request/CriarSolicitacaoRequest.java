package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CriarSolicitacaoRequest(
    @NotBlank(message = "O título é obrigatório.")
    String titulo,

    @NotBlank(message = "A descrição é obrigatória.")
    String descricao,

    @NotBlank(message = "A justificativa é obrigatória.")
    String justificativa,

    @NotBlank(message = "O tipo é obrigatório.")
    String tipo,

    @NotNull(message = "O ID do projeto é obrigatório.")
    Long projetoId,

    Long materialId,
    Integer quantidade,
    BigDecimal valor
) {}
