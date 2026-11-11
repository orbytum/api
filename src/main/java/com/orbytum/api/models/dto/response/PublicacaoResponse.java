package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.entity.Publicacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicacaoResponse(
        Long id,
        UUID uuid,
        String titulo,
        String descricao,
        Long projetoId,
        String projetoTitulo,
        String url,
        LocalDateTime dthRegistro,
        boolean isAtivo
) {
    public static PublicacaoResponse fromEntity(Publicacao publicacao) {
        if (publicacao == null) {
            return null;
        }

        Long projetoId = (publicacao.getProjeto() != null) ? publicacao.getProjeto().getId() : null;
        String projetoTitulo = (publicacao.getProjeto() != null) ? publicacao.getProjeto().getTitulo() : null;

        return new PublicacaoResponse(
                publicacao.getId(),
                publicacao.getUuid(),
                publicacao.getTitulo(),
                publicacao.getDescricao(),
                projetoId,
                projetoTitulo,
                publicacao.getUrl(),
                publicacao.getDthRegistro(),
                publicacao.isAtivo()
        );
    }
}
