package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
import com.orbytum.api.models.enums.ProjetoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditProjetoRequest (
    @NotNull(message = "O status do projeto é obrigatório")
    ProjetoStatus status,

    @NotBlank(message = "O título do projeto é obrigatório")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
    String titulo,

    @NotBlank(message = "O assunto do projeto é obrigatório")
    String assunto,

    @NotNull(message = "O status de ativo é obrigatório")
    Boolean isAtivo
) {}
