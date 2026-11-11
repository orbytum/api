package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record CreatePublicacaoRequest(
        @NotBlank(message = "O título da publicação é obrigatório.")
        String titulo,

        @NotBlank(message = "A descrição da publicação é obrigatória.")
        String descricao,

        @NotNull(message = "O ID do projeto vinculado é obrigatório.")
        Long projetoId,

        @NotNull(message = "O arquivo PDF da publicação é obrigatório.")
        MultipartFile arquivo
) {}
