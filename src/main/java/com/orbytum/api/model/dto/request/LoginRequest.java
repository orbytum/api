package com.orbytum.api.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail em formato inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {}
