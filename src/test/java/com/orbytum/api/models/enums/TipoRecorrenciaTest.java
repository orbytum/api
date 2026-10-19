package com.orbytum.api.models.enums;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TipoRecorrenciaTest {

    private final LocalDateTime agora = LocalDateTime.of(2026, 10, 19, 12, 0);

    @Test
    void nenhumaMantemADataOriginal() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 9, 0);
        assertEquals(base, TipoRecorrencia.NENHUMA.proximaOcorrencia(base, agora));
    }

    @Test
    void dataFuturaNaoMuda() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 9, 0);
        assertEquals(base, TipoRecorrencia.SEMANAL.proximaOcorrencia(base, agora));
    }

    @Test
    void diariaVaiParaProximoDia() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 9, 0);
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), TipoRecorrencia.DIARIA.proximaOcorrencia(base, agora));
    }

    @Test
    void semanalMantemODiaDaSemana() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 19, 14, 0); // quarta
        assertEquals(LocalDateTime.of(2026, 10, 21, 14, 0), TipoRecorrencia.SEMANAL.proximaOcorrencia(base, agora));
    }

    @Test
    void mensalNaoPerdeODia31() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 31, 8, 0);
        assertEquals(LocalDateTime.of(2026, 10, 31, 8, 0), TipoRecorrencia.MENSAL.proximaOcorrencia(base, agora));
    }

    @Test
    void anualVaiParaProximoAno() {
        LocalDateTime base = LocalDateTime.of(2025, 12, 1, 0, 0);
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), TipoRecorrencia.ANUAL.proximaOcorrencia(base, agora));
    }

    @Test
    void recorrenteNuncaFicaEncerrado() {
        LocalDateTime base = LocalDateTime.now().minusMonths(3);
        LocalDateTime proxima = TipoRecorrencia.SEMANAL.proximaOcorrencia(base, LocalDateTime.now());
        assertNotEquals(StatusLembrete.ENCERRADO, StatusLembrete.calcularStatus(proxima));
    }
}
