package com.orbytum.api.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.Normalizer;

@AllArgsConstructor
public enum TipoRecorrencia {
    NENHUMA("Nenhuma"),
    DIARIA("Diária"),
    SEMANAL("Semanal"),
    MENSAL("Mensal"),
    ANUAL("Anual");

    private final String descricao;

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static TipoRecorrencia fromString(String value) {
        if (value == null || value.isBlank()) {
            return NENHUMA;
        }
        String normalizedInput = normalizar(value);
        for (TipoRecorrencia rec : TipoRecorrencia.values()) {
            if (normalizar(rec.name()).equals(normalizedInput) ||
                normalizar(rec.descricao).equals(normalizedInput)) {
                return rec;
            }
        }
        throw new IllegalArgumentException("Tipo de recorrência inválido: " + value + ". Valores aceitos: Nenhuma, Diária, Semanal, Mensal, Anual.");
    }

    private static String normalizar(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase();
    }
}
