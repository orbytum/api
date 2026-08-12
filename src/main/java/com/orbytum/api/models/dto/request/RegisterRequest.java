package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest (
        @NotBlank String nome,

        @NotBlank
        @Email String email,

        @NotBlank String senha,

        @NotBlank String telefone,

        @NotBlank String titulo

) {}
