package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SolicitacaoStatus {
    PENDENTE(1),
    EM_ANDAMENTO(2),
    CONCLUIDA(3),
    ENCERRADA(4),
    REJEITADA(5);

    @Getter
    private Integer id;

    public static SolicitacaoStatus fromId(Integer id) {
        for (SolicitacaoStatus s : SolicitacaoStatus.values()) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Id de status de solicitacao invalido: " + id);
    }
}
