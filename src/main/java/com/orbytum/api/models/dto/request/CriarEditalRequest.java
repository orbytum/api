package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CriarEditalRequest(
        @NotBlank(message = "O organizador do edital é obrigatório.")
        String organizador,

        @NotBlank(message = "A descrição do edital é obrigatória.")
        String descricao,

        BigDecimal valor,

        @NotBlank(message = "O link (URI) do edital é obrigatório.")
        @Pattern(regexp = "^https?://.+", message = "A URI deve começar com http:// ou https://.")
        String uri,

        @NotNull(message = "A data de abertura é obrigatória.")
        LocalDateTime dthAbertura,

        @NotNull(message = "A data de encerramento é obrigatória.")
        LocalDateTime dthEncerramento
) {}
