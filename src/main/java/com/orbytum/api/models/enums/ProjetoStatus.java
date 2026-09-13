package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ProjetoStatus {
    EM_ANDAMENTO(1), CONCLUIDO(2);

    @Getter
    private Integer id;

    public static ProjetoStatus fromId(Integer id) {
        for (ProjetoStatus p : ProjetoStatus.values()) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new IllegalArgumentException("Id de status de projeto invalido: " + id);
    }
}
