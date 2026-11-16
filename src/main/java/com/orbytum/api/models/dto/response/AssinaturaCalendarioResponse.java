package com.orbytum.api.models.dto.response;

/**
 * REQ-069: link de assinatura (iCalendar) da agenda do grupo, para Google Calendar, Outlook e similares.
 *
 * @param url endereço HTTP(S) do feed .ics, válido enquanto o usuário tiver acesso ao grupo
 */
public record AssinaturaCalendarioResponse(String url) {}
