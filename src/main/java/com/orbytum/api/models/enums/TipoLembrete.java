package com.orbytum.api.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.Normalizer;

@AllArgsConstructor
public enum TipoLembrete {
    EDITAL("Edital"),
    REUNIAO("Reunião"),
    APRESENTACAO("Apresentação"),
    WORKSHOP("Workshop");

    private final String descricao;

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static TipoLembrete fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizedInput = normalizar(value);
        for (TipoLembrete tipo : TipoLembrete.values()) {
            if (normalizar(tipo.name()).equals(normalizedInput) ||
                normalizar(tipo.descricao).equals(normalizedInput)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de lembrete inválido: " + value + ". Valores aceitos: Edital, Reunião, Apresentação, Workshop.");
    }

    private static String normalizar(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase();
    }
}
