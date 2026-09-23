package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TipoRecorrencia {
    NENHUMA("Nenhuma"),
    DIARIA("Diária"),
    SEMANAL("Semanal"),
    MENSAL("Mensal"),
    ANUAL("Anual");

    @Getter
    private final String descricao;

    public static TipoRecorrencia fromString(String value) {
        if (value == null || value.isBlank()) return NENHUMA;
        for (TipoRecorrencia rec : TipoRecorrencia.values()) {
            if (rec.name().equalsIgnoreCase(value) || rec.getDescricao().equalsIgnoreCase(value)) {
                return rec;
            }
        }
        return NENHUMA;
    }
}
