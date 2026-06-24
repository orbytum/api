package com.orbytum.api.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum AtividadeStatus {
    A_FAZER(1), EM_ANDAMENTO(2), FINALIZADA(3), TRAVADA(4);

    @Getter private Integer id;

    public static AtividadeStatus fromId(Integer id) {
        for (AtividadeStatus p : AtividadeStatus.values()) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new IllegalArgumentException("Id de status de atividade invalido: " + id);
    }
}
