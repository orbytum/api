package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum MaterialStatus {
    DISPONIVEL(1), MANUTENCAO(2), EM_USO(3);

    @Getter private Integer id;

    public static MaterialStatus fromId(Integer id) {
        for (MaterialStatus p : MaterialStatus.values()) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new IllegalArgumentException("Id de status de material invalido: " + id);
    }
}
