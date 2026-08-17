package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TipoConvite {
    DIRETO("direto"),
    LINK_EXCLUSIVO("link_exclusivo");

    @Getter
    private final String key;

    public static TipoConvite fromKey(String key) {
        for (TipoConvite tipo : TipoConvite.values()) {
            if (tipo.getKey().equalsIgnoreCase(key)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de convite inválido: " + key);
    }
}
