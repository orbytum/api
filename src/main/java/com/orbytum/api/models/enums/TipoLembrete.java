package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TipoLembrete {
    EDITAL("Edital"),
    REUNIAO("Reunião"),
    APRESENTACAO("Apresentação"),
    WORKSHOP("Workshop");

    @Getter
    private final String descricao;

    public static TipoLembrete fromString(String value) {
        if (value == null) return null;
        for (TipoLembrete tipo : TipoLembrete.values()) {
            if (tipo.name().equalsIgnoreCase(value) || tipo.getDescricao().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de lembrete inválido: " + value);
    }
}
