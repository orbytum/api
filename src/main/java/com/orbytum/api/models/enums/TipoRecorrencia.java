package com.orbytum.api.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    /**
     * Próxima ocorrência (>= agora) a partir da data base. Sem recorrência, ou com base
     * ainda no futuro, retorna a própria data base. Meses/anos são calculados a partir da
     * base (e não da ocorrência anterior) para não perder o dia, ex.: 31 -> 28 -> 31.
     */
    public LocalDateTime proximaOcorrencia(LocalDateTime base, LocalDateTime agora) {
        if (base == null || this == NENHUMA || !base.isBefore(agora)) {
            return base;
        }
        switch (this) {
            case DIARIA:
            case SEMANAL: {
                ChronoUnit unidade = this == DIARIA ? ChronoUnit.DAYS : ChronoUnit.WEEKS;
                long passos = unidade.between(base, agora);
                LocalDateTime candidata = base.plus(passos, unidade);
                return candidata.isBefore(agora) ? candidata.plus(1, unidade) : candidata;
            }
            default: {
                int mesesPorPasso = this == MENSAL ? 1 : 12;
                long k = Math.max(0, ChronoUnit.MONTHS.between(base, agora) / mesesPorPasso);
                LocalDateTime candidata = base.plusMonths(k * mesesPorPasso);
                while (candidata.isBefore(agora)) {
                    k++;
                    candidata = base.plusMonths(k * mesesPorPasso);
                }
                return candidata;
            }
        }
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
