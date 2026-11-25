package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CriarSolicitacaoRequest(
    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
    String titulo,

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
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
