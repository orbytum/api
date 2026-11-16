package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record CreatePublicacaoRequest(
        @NotBlank(message = "O título da publicação é obrigatório.")
        String titulo,

        @NotBlank(message = "A descrição da publicação é obrigatória.")
        String descricao,

        @NotNull(message = "O ID do projeto vinculado é obrigatório.")
        Long projetoId,

        @NotNull(message = "O arquivo PDF da publicação é obrigatório.")
        MultipartFile arquivo,

        /* REQ-088: tipo (ex.: Artigo, Livro, Tese), data da publicação e link externo (ex.: DOI). */
        @NotBlank(message = "O tipo da publicação é obrigatório.")
        @Size(max = 20, message = "O tipo da publicação deve ter no máximo 20 caracteres.")
        String tipo,

        @NotNull(message = "A data da publicação é obrigatória.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataPublicacao,

        @Size(max = 500, message = "O link deve ter no máximo 500 caracteres.")
        String link
) {}
