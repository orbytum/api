package com.orbytum.api.util;

import com.orbytum.api.models.entity.Evento;
import com.orbytum.api.models.entity.EventoParticipante;
import com.orbytum.api.models.enums.TipoRecorrencia;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera um arquivo iCalendar (RFC 5545) com os lembretes/eventos de um grupo.
 *
 * <p>As datas são exportadas como "floating time" (sem sufixo Z), pois o sistema
 * armazena {@link LocalDateTime} sem fuso horário. Assim o cliente de calendário
 * interpreta o horário no fuso local do usuário.</p>
 */
public final class IcsCalendarBuilder {

    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter FORMATO_DATA_HORA_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private IcsCalendarBuilder() {
    }

    /**
     * Monta o conteúdo do arquivo .ics.
     *
     * @param nomeCalendario nome exibido na agenda externa (normalmente o nome do grupo)
     * @param eventos        lembretes/eventos ativos do grupo
     * @return bytes do arquivo em UTF-8
     */
    public static byte[] gerar(String nomeCalendario, List<Evento> eventos) {
        StringBuilder ics = new StringBuilder();

        adicionarLinha(ics, "BEGIN:VCALENDAR");
        adicionarLinha(ics, "VERSION:2.0");
        adicionarLinha(ics, "PRODID:-//Orbytum//Lembretes//PT-BR");
        adicionarLinha(ics, "CALSCALE:GREGORIAN");
        adicionarLinha(ics, "METHOD:PUBLISH");
        adicionarLinha(ics, "X-WR-CALNAME:" + escapar(nomeCalendario));
        adicionarLinha(ics, "X-WR-CALDESC:" + escapar("Lembretes e eventos do grupo " + nomeCalendario));

        for (Evento evento : eventos) {
            adicionarEvento(ics, evento);
        }

        adicionarLinha(ics, "END:VCALENDAR");

        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void adicionarEvento(StringBuilder ics, Evento evento) {
        if (evento.getId() == null || evento.getDthInicio() == null) {
            return;
        }

        LocalDateTime inicio = evento.getDthInicio();
        LocalDateTime fim = evento.getDthFim();
        if (fim == null || !fim.isAfter(inicio)) {
            fim = inicio.plusHours(1);
        }

        adicionarLinha(ics, "BEGIN:VEVENT");
        adicionarLinha(ics, "UID:evento-" + evento.getId() + "@orbytum.com");
        adicionarLinha(ics, "DTSTAMP:" + FORMATO_DATA_HORA_UTC.format(Instant.now().atOffset(ZoneOffset.UTC)));
        adicionarLinha(ics, "DTSTART:" + FORMATO_DATA_HORA.format(inicio));
        adicionarLinha(ics, "DTEND:" + FORMATO_DATA_HORA.format(fim));
        adicionarLinha(ics, "SUMMARY:" + escapar(evento.getTitulo()));
        adicionarLinha(ics, "DESCRIPTION:" + escapar(evento.getDescricao()));

        if (textoPreenchido(evento.getLocalizacao())) {
            adicionarLinha(ics, "LOCATION:" + escapar(evento.getLocalizacao()));
        }
        if (textoPreenchido(evento.getUrl())) {
            adicionarLinha(ics, "URL:" + escapar(evento.getUrl()));
        }
        if (evento.getTipo() != null) {
            adicionarLinha(ics, "CATEGORIES:" + escapar(evento.getTipo().getDescricao()));
            adicionarLinha(ics, "X-ORBYTUM-SUBTIPO:" + evento.getTipo().name());
        }

        adicionarLinha(ics, "X-ORBYTUM-TIPO:LEMBRETE");
        if (textoPreenchido(evento.getOrganizador())) {
            adicionarLinha(ics, "X-ORBYTUM-ORGANIZADOR:" + escapar(evento.getOrganizador()));
            if (evento.getCriador() != null && textoPreenchido(evento.getCriador().getEmail())) {
                adicionarLinha(ics, "ORGANIZER;CN=" + escapar(evento.getOrganizador())
                        + ":mailto:" + evento.getCriador().getEmail());
            }
        }

        String rrule = mapearRecorrencia(evento.getRecorrencia());
        if (rrule != null) {
            adicionarLinha(ics, "RRULE:" + rrule);
        }

        adicionarParticipantes(ics, evento.getParticipantes());

        adicionarLinha(ics, "STATUS:CONFIRMED");
        adicionarLinha(ics, "TRANSP:OPAQUE");
        adicionarLinha(ics, "END:VEVENT");
    }

    private static void adicionarParticipantes(StringBuilder ics, List<EventoParticipante> participantes) {
        if (participantes == null) {
            return;
        }
        for (EventoParticipante participante : participantes) {
            if (participante.getUsuario() == null || !textoPreenchido(participante.getUsuario().getEmail())) {
                continue;
            }
            String partstat = participante.isPresencaConfirmada() ? "ACCEPTED" : "NEEDS-ACTION";
            adicionarLinha(ics, "ATTENDEE;CN=" + escapar(participante.getNome())
                    + ";ROLE=REQ-PARTICIPANT;PARTSTAT=" + partstat
                    + ":mailto:" + participante.getUsuario().getEmail());
        }
    }

    private static String mapearRecorrencia(TipoRecorrencia recorrencia) {
        if (recorrencia == null) {
            return null;
        }
        return switch (recorrencia) {
            case DIARIA -> "FREQ=DAILY";
            case SEMANAL -> "FREQ=WEEKLY";
            case MENSAL -> "FREQ=MONTHLY";
            case ANUAL -> "FREQ=YEARLY";
            case NENHUMA -> null;
        };
    }

    private static boolean textoPreenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Escapa os caracteres especiais de texto definidos na RFC 5545 e normaliza
     * quebras de linha para {@code \n}.
     */
    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("\\", "\\\\")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n")
                .replace(";", "\\;")
                .replace(",", "\\,");
    }

    /**
     * Adiciona uma linha respeitando o limite de 75 octetos por linha física.
     * Linhas maiores são dobradas; as continuações começam com um espaço.
     */
    private static void adicionarLinha(StringBuilder ics, String linha) {
        byte[] bytes = linha.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 75) {
            ics.append(linha).append(CRLF);
            return;
        }

        StringBuilder atual = new StringBuilder();
        int octetos = 0;
        for (int i = 0; i < linha.length(); ) {
            int codePoint = linha.codePointAt(i);
            String caractere = new String(Character.toChars(codePoint));
            int tamanho = caractere.getBytes(StandardCharsets.UTF_8).length;

            if (octetos + tamanho > 75) {
                ics.append(atual).append(CRLF).append(' ');
                atual.setLength(0);
                octetos = 1; // o espaço inicial da continuação também conta
            }

            atual.append(caractere);
            octetos += tamanho;
            i += Character.charCount(codePoint);
        }

        ics.append(atual).append(CRLF);
    }
}
