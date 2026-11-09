package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CreatePublicacaoRequest;
import com.orbytum.api.models.dto.request.EditarPublicacaoRequest;
import com.orbytum.api.models.dto.response.PublicacaoPaginadaResponse;
import com.orbytum.api.models.dto.response.PublicacaoResponse;
import com.orbytum.api.service.PublicacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/publicacoes")
@RequiredArgsConstructor
public class PublicacaoController {

    private final PublicacaoService publicacaoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacaoResponse> criarPublicacao(
            @Valid @ModelAttribute CreatePublicacaoRequest request,
            Authentication authentication) {
        PublicacaoResponse response = publicacaoService.criarPublicacao(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacaoResponse> atualizarPublicacao(
            @PathVariable Long id,
            @Valid @ModelAttribute EditarPublicacaoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(publicacaoService.atualizarPublicacao(id, request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<PublicacaoPaginadaResponse> listarPublicacoes(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long projetoId,
            @RequestParam(required = false) Long grupoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        PublicacaoPaginadaResponse response = publicacaoService.listarPublicacoes(
                titulo, dataInicio, dataFim, projetoId, grupoId, page, size, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacaoResponse> buscarPorId(
            @PathVariable Long id,
            Authentication authentication) {
        PublicacaoResponse response = publicacaoService.buscarPorId(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<PublicacaoResponse> buscarPorUuid(
            @PathVariable UUID uuid,
            Authentication authentication) {
        PublicacaoResponse response = publicacaoService.buscarPorUuid(uuid, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarPublicacao(
            @PathVariable Long id,
            Authentication authentication) {
        publicacaoService.inativarPublicacao(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<Void> inativarPublicacaoPatch(
            @PathVariable Long id,
            Authentication authentication) {
        publicacaoService.inativarPublicacao(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
