package com.orbytum.api.controller;

import com.orbytum.api.service.LembreteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REQ-069: feed iCalendar público consultado por Google Calendar, Outlook e similares.
 * A autenticação é feita pelo token assinado no próprio link, pois esses serviços não enviam cabeçalhos.
 */
@RestController
@RequestMapping("/calendario")
@RequiredArgsConstructor
@Tag(name = "Calendário", description = "Assinatura da agenda do grupo em aplicativos externos")
public class CalendarioController {

    private final LembreteService lembreteService;

    @GetMapping(value = "/feed/{token}", produces = "text/calendar")
    @Operation(summary = "Feed iCalendar da agenda do grupo (link de assinatura)")
    public ResponseEntity<byte[]> feed(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(lembreteService.exportarIcsAssinatura(token));
    }
}
