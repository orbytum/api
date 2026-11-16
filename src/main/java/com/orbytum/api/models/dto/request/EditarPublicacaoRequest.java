package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record EditarPublicacaoRequest(
        @NotBlank(message = "O título da publicação é obrigatório.")
        String titulo,

        @NotBlank(message = "A descrição da publicação é obrigatória.")
        String descricao,

        Long projetoId,

        MultipartFile arquivo,

        /* REQ-088: quando nulos, tipo, data e link mantêm os valores atuais. */
        @Size(max = 20, message = "O tipo da publicação deve ter no máximo 20 caracteres.")
        String tipo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataPublicacao,

        @Size(max = 500, message = "O link deve ter no máximo 500 caracteres.")
        String link
) {
}
