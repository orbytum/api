package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public record EditNoticiaRequest(
        @NotBlank(message = "O título da notícia é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @NotBlank(message = "O conteúdo da notícia é obrigatório.")
        String conteudo,

        String referencia,

        @NotBlank(message = "O assunto da notícia é obrigatório.")
        String assunto,

        LocalDateTime dataInicioExibicao,

        LocalDateTime dataFimExibicao,

        List<Long> projetoIds
) {}
