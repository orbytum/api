package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CreatePublicacaoRequest;
import com.orbytum.api.models.dto.response.PublicacaoPaginadaResponse;
import com.orbytum.api.models.dto.response.PublicacaoResponse;
import com.orbytum.api.service.PublicacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/publicacoes")
@RequiredArgsConstructor
public class PublicacaoController {

    private final PublicacaoService publicacaoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacaoResponse> criarPublicacao(@Valid @ModelAttribute CreatePublicacaoRequest request) {
        PublicacaoResponse response = publicacaoService.criarPublicacao(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PublicacaoPaginadaResponse> listarPublicacoes(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long projetoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PublicacaoPaginadaResponse response = publicacaoService.listarPublicacoes(titulo, dataInicio, dataFim, projetoId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacaoResponse> buscarPorId(@PathVariable Long id) {
        PublicacaoResponse response = publicacaoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<PublicacaoResponse> buscarPorUuid(@PathVariable UUID uuid) {
        PublicacaoResponse response = publicacaoService.buscarPorUuid(uuid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarPublicacao(@PathVariable Long id) {
        publicacaoService.inativarPublicacao(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<Void> inativarPublicacaoPatch(@PathVariable Long id) {
        publicacaoService.inativarPublicacao(id);
        return ResponseEntity.noContent().build();
    }
}
