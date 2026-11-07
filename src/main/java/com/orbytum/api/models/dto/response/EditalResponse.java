package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.entity.Edital;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EditalResponse(
        Long id,
        String organizador,
        String descricao,
        BigDecimal valor,
        String uri,
        LocalDateTime dthAbertura,
        LocalDateTime dthEncerramento
) {
    public static EditalResponse fromEntity(Edital edital) {
        if (edital == null) {
            return null;
        }
        return new EditalResponse(
                edital.getId(),
                edital.getOrganizador(),
                edital.getDescricao(),
                edital.getValor(),
                edital.getUri(),
                edital.getDthAbertura(),
                edital.getDthEncerramento());
    }
}
