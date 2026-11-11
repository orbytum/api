package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.entity.Noticia;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.enums.NoticiaStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record NoticiaResponse(
        Long id,
        UUID uuid,
        Long grupoId,
        String grupoNome,
        Long autorId,
        String autorNome,
        Long aprovadorId,
        String titulo,
        String conteudo,
        String referencia,
        String assunto,
        LocalDateTime dataInicioExibicao,
        LocalDateTime dataFimExibicao,
        String motivoRejeicao,
        NoticiaStatus status,
        LocalDateTime dthRegistro,
        LocalDateTime dthAprovacao,
        boolean isAtivo,
        boolean isExpirada,
        List<ProjetoRotuloResponse> projetos
) {
    public static NoticiaResponse fromEntity(Noticia noticia, boolean expirada) {
        if (noticia == null) {
            return null;
        }

        List<ProjetoRotuloResponse> projetos = noticia.getProjetos() == null
                ? List.of()
                : noticia.getProjetos().stream()
                .sorted(Comparator.comparing(Projeto::getTitulo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(ProjetoRotuloResponse::fromEntity)
                .toList();

        return new NoticiaResponse(
                noticia.getId(),
                noticia.getUuid(),
                noticia.getGrupo() != null ? noticia.getGrupo().getId() : null,
                noticia.getGrupo() != null ? noticia.getGrupo().getNome() : null,
                noticia.getAutor() != null ? noticia.getAutor().getId() : null,
                noticia.getAutor() != null ? noticia.getAutor().getNome() : null,
                noticia.getAprovador() != null ? noticia.getAprovador().getId() : null,
                noticia.getTitulo(),
                noticia.getConteudo(),
                noticia.getReferencia(),
                noticia.getAssunto(),
                noticia.getDataInicioExibicao(),
                noticia.getDataFimExibicao(),
                noticia.getMotivoRejeicao(),
                noticia.getStatus(),
                noticia.getDthRegistro(),
                noticia.getDthAprovacao(),
                noticia.isAtivo(),
                expirada,
                projetos
        );
    }
}
