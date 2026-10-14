package com.orbytum.api.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.Normalizer;
import java.time.LocalDateTime;

@AllArgsConstructor
public enum StatusLembrete {
    EM_ANDAMENTO("Em Andamento"),
    PROXIMO("Próximo"),
    ENCERRADO("Encerrado");

    private final String descricao;

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    public static StatusLembrete calcularStatus(LocalDateTime dataHora) {
        if (dataHora == null) {
            return ENCERRADO;
        }
        LocalDateTime agora = LocalDateTime.now();
        if (dataHora.isBefore(agora)) {
            return ENCERRADO;
        }
        if (dataHora.isBefore(agora.plusDays(3).plusSeconds(1))) {
            return PROXIMO;
        }
        return EM_ANDAMENTO;
    }

    @JsonCreator
    public static StatusLembrete fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizedInput = normalizar(value);
        for (StatusLembrete status : StatusLembrete.values()) {
            if (normalizar(status.name()).equals(normalizedInput) ||
                normalizar(status.descricao).equals(normalizedInput)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de lembrete inválido: " + value + ". Valores aceitos: Em Andamento, Próximo, Encerrado.");
    }

    private static String normalizar(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase();
    }
}
