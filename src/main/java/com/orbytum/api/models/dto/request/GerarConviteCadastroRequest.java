package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotNull;

public record GerarConviteCadastroRequest(
        @NotNull(message = "O Email é obrigatório")
        String email,
        Integer diasValidade
) { }