package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SolicitacaoStatus {
    /** Aguardando a aprovação do Líder do grupo. */
    PENDENTE(1),
    EM_ANDAMENTO(2),
    CONCLUIDA(3),
    ENCERRADA(4),
    REJEITADA(5),
    /** Aprovada pelo Líder do grupo, aguardando a decisão do administrador. */
    AGUARDANDO_ADMINISTRADOR(6);

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
