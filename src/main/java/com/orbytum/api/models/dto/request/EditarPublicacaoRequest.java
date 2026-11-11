package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public record EditarPublicacaoRequest(
        @NotBlank(message = "O título da publicação é obrigatório.")
        String titulo,

        @NotBlank(message = "A descrição da publicação é obrigatória.")
        String descricao,

        Long projetoId,

        MultipartFile arquivo
) {
}
