package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.entity.Documento;

import java.time.LocalDateTime;

public record DocumentoResponse(
        Long id,
        String nome,
        String extensao,
        String contentType,
        Long tamanho,
        LocalDateTime dthCriacao
) {
    public static DocumentoResponse fromEntity(Documento documento) {
        if (documento == null) {
            return null;
        }
        return new DocumentoResponse(
                documento.getId(),
                documento.getNome(),
                documento.getExtensao(),
                documento.getContentType(),
                documento.getTamanho(),
                documento.getDthCriacao());
    }
}
