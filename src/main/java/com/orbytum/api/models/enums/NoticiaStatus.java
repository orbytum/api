package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum NoticiaStatus {
    PENDENTE(1),
    APROVADA(2),
    REJEITADA(3);

    @Getter
    private Integer id;

    public static NoticiaStatus fromId(Integer id) {
        for (NoticiaStatus status : NoticiaStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Id de status de notícia inválido: " + id);
    }

    public boolean isPendente() {
        return this == PENDENTE;
    }

    public boolean isAprovada() {
        return this == APROVADA;
    }
}
