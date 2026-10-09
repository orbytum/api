package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.MaterialStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CriarMaterialRequest(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "A descrição é obrigatória.")
        String descricao,

        @NotBlank(message = "O identificador é obrigatório.")
        String identificador,

        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 1, message = "A quantidade deve ser maior que zero.")
        Integer quantidade,

        @NotBlank(message = "A localização é obrigatória.")
        String localizacao,

        @NotNull(message = "A data de aquisição é obrigatória.")
        LocalDateTime dthCompra,

        MaterialStatus status
) {}
