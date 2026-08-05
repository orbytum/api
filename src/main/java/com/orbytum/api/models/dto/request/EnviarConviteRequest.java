package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarConviteRequest(
        @NotNull(message = "O ID do grupo é obrigatório")
        Long grupoId,

        @NotBlank(message = "O e-mail do convidado é obrigatório")
        @Email(message = "E-mail em formato inválido")
        String emailConvidado
) {}
