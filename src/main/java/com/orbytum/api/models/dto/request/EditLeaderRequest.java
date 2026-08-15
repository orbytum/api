package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditLeaderRequest(
    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotBlank(message = "O número de telefone é obrigatório")
    String telefone,

    @NotBlank(message = "O título é obrigatório")
    String titulo
) {}
