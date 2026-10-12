package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CreateNoticiaRequest;
import com.orbytum.api.models.dto.request.EditNoticiaRequest;
import com.orbytum.api.models.dto.request.RejeitarNoticiaRequest;
import com.orbytum.api.models.dto.response.NoticiaPaginadaResponse;
import com.orbytum.api.models.dto.response.NoticiaResponse;
import com.orbytum.api.service.NoticiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/noticias")
@RequiredArgsConstructor
public class NoticiaController {

    private final NoticiaService noticiaService;

    @PostMapping
    public ResponseEntity<NoticiaResponse> criarNoticia(
            @Valid @RequestBody CreateNoticiaRequest request,
            Authentication authentication) {
        NoticiaResponse response = noticiaService.criar(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<NoticiaPaginadaResponse> listarNoticias(
            @RequestParam Long grupoId,
            @RequestParam(required = false) String assunto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        NoticiaPaginadaResponse response = noticiaService.listar(
                grupoId, assunto, dataInicio, dataFim, page, size, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pendentes")
    public ResponseEntity<NoticiaPaginadaResponse> listarPendentes(
            @RequestParam Long grupoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        NoticiaPaginadaResponse response = noticiaService.listarPendentes(
                grupoId, page, size, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticiaResponse> buscarNoticia(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(noticiaService.buscar(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoticiaResponse> atualizarNoticia(
            @PathVariable Long id,
            @Valid @RequestBody EditNoticiaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(noticiaService.atualizar(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<NoticiaResponse> aprovarNoticia(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(noticiaService.aprovar(id, authentication.getName()));
    }

    @PatchMapping("/{id}/rejeitar")
    public ResponseEntity<NoticiaResponse> rejeitarNoticia(
            @PathVariable Long id,
            @RequestBody(required = false) RejeitarNoticiaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(noticiaService.rejeitar(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarNoticia(
            @PathVariable Long id,
            Authentication authentication) {
        noticiaService.deletar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
