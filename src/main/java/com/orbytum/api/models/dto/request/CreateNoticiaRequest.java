package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateNoticiaRequest(
        @NotNull(message = "O ID do grupo é obrigatório.")
        Long grupoId,

        @NotBlank(message = "O título da notícia é obrigatório.")
        String titulo,

        @NotBlank(message = "O conteúdo da notícia é obrigatório.")
        String conteudo,

        String referencia,

        @NotBlank(message = "O assunto da notícia é obrigatório.")
        String assunto,

        LocalDateTime dataInicioExibicao,

        LocalDateTime dataFimExibicao
) {}
