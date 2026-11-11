package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.entity.Edital;
import com.orbytum.api.models.entity.Projeto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record EditalResponse(
        Long id,
        String organizador,
        String descricao,
        BigDecimal valor,
        String uri,
        LocalDateTime dthAbertura,
        LocalDateTime dthEncerramento,
        List<ProjetoRotuloResponse> projetos
) {
    public static EditalResponse fromEntity(Edital edital) {
        return fromEntity(edital, null);
    }

    public static EditalResponse fromEntity(Edital edital, Long grupoId) {
        if (edital == null) {
            return null;
        }

        List<ProjetoRotuloResponse> projetos = edital.getProjetos() == null
                ? List.of()
                : edital.getProjetos().stream()
                .filter(projeto -> grupoId == null
                        || (projeto.getGrupo() != null && grupoId.equals(projeto.getGrupo().getId())))
                .sorted(Comparator.comparing(Projeto::getTitulo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(ProjetoRotuloResponse::fromEntity)
                .toList();

        return new EditalResponse(
                edital.getId(),
                edital.getOrganizador(),
                edital.getDescricao(),
                edital.getValor(),
                edital.getUri(),
                edital.getDthAbertura(),
                edital.getDthEncerramento(),
                projetos);
    }
}
