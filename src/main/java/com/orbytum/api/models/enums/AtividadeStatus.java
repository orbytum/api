package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum AtividadeStatus {
    PENDENTE(1),
    EM_ANDAMENTO(2),
    AGUARDANDO_CONFIRMACAO(3),
    CONCLUIDA(4),
    ENCERRADA(5);

    @Getter
    private Integer id;

    public static AtividadeStatus fromId(Integer id) {
        for (AtividadeStatus p : AtividadeStatus.values()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Id de status de atividade invalido: " + id);
    }

    public boolean isAberta() {
        return this != CONCLUIDA && this != ENCERRADA;
    }

    public boolean podeTransicionarPara(AtividadeStatus destino) {
        if (destino == null || destino == this) {
            return false;
        }
        return switch (this) {
            case PENDENTE -> destino == EM_ANDAMENTO;
            case EM_ANDAMENTO -> destino == AGUARDANDO_CONFIRMACAO;
            case AGUARDANDO_CONFIRMACAO -> destino == CONCLUIDA;
            case CONCLUIDA -> destino == ENCERRADA;
            case ENCERRADA -> false;
        };
    }
}
