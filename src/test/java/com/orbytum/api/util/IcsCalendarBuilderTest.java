package com.orbytum.api.util;

import com.orbytum.api.models.entity.Evento;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IcsCalendarBuilderTest {

    private Evento criarEvento() {
        Usuario criador = new Usuario(
                "Carlos Pesquisador",
                "criador@orbytum.com",
                "1199999999",
                "Dr"
        );

        return Evento.builder()
                .id(1L)
                .grupo(new Grupo("Grupo de Inteligência Artificial", true))
                .criador(criador)
                .titulo("Reunião; planejamento, 2026")
                .descricao("Linha 1\nLinha 2")
                .tipo(TipoLembrete.REUNIAO)
                .dthInicio(LocalDateTime.of(2026, 11, 2, 14, 30))
                .dthFim(LocalDateTime.of(2026, 11, 2, 15, 30))
                .localizacao("Sala 1, Bloco B")
                .url("https://meet.google.com/abc")
                .recorrencia(TipoRecorrencia.SEMANAL)
                .organizador("Carlos Pesquisador")
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .participantes(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Deve gerar um VCALENDAR com os dados do lembrete e recorrência")
    void deveGerarIcsComDadosDoLembrete() {
        byte[] bytes = IcsCalendarBuilder.gerar("Grupo; IA, 2026", List.of(criarEvento()));
        String ics = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertTrue(ics.contains("X-WR-CALNAME:Grupo\\; IA\\, 2026\r\n"));
        assertTrue(ics.contains("BEGIN:VEVENT\r\n"));
        assertTrue(ics.contains("UID:evento-1@orbytum.com\r\n"));
        assertTrue(ics.contains("DTSTART:20261102T143000\r\n"));
        assertTrue(ics.contains("DTEND:20261102T153000\r\n"));
        assertTrue(ics.contains("SUMMARY:Reunião\\; planejamento\\, 2026\r\n"));
        assertTrue(ics.contains("DESCRIPTION:Linha 1\\nLinha 2\r\n"));
        assertTrue(ics.contains("LOCATION:Sala 1\\, Bloco B\r\n"));
        assertTrue(ics.contains("CATEGORIES:Reunião\r\n"));
        assertTrue(ics.contains("X-ORBYTUM-TIPO:LEMBRETE\r\n"));
        assertTrue(ics.contains("X-ORBYTUM-SUBTIPO:REUNIAO\r\n"));
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY\r\n"));
        assertTrue(ics.contains("STATUS:CONFIRMED\r\n"));
        assertTrue(ics.contains("END:VEVENT\r\n"));
        assertFalse(ics.contains("RRULE:FREQ=NONE"));
    }

    @Test
    @DisplayName("Deve dobrar linhas que ultrapassam 75 octetos")
    void deveDobrarLinhasLongas() {
        Evento evento = criarEvento();
        evento.setDescricao("á".repeat(100));

        String ics = new String(IcsCalendarBuilder.gerar("Grupo", List.of(evento)), StandardCharsets.UTF_8);

        for (String linha : ics.split("\r\n")) {
            assertTrue(
                    linha.getBytes(StandardCharsets.UTF_8).length <= 75,
                    "Linha excedeu 75 octetos: " + linha
            );
        }
    }

    @Test
    @DisplayName("Não deve gerar RRULE para lembrete sem recorrência")
    void naoDeveGerarRruleSemRecorrencia() {
        Evento evento = criarEvento();
        evento.setRecorrencia(TipoRecorrencia.NENHUMA);

        String ics = new String(IcsCalendarBuilder.gerar("Grupo", List.of(evento)), StandardCharsets.UTF_8);

        assertFalse(ics.contains("RRULE:"));
    }
}
