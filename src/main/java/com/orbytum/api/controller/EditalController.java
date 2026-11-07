package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CriarEditalRequest;
import com.orbytum.api.models.dto.request.EditarEditalRequest;
import com.orbytum.api.models.dto.response.EditalPaginadoResponse;
import com.orbytum.api.models.dto.response.EditalResponse;
import com.orbytum.api.service.EditalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/grupos/{grupoId}/editais")
@RequiredArgsConstructor
public class EditalController {

    private final EditalService editalService;

    @PostMapping
    public ResponseEntity<EditalResponse> criar(
            @PathVariable Long grupoId,
            @Valid @RequestBody CriarEditalRequest request,
            Authentication authentication) {
        EditalResponse response = editalService.criar(grupoId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<EditalPaginadoResponse> listar(
            @PathVariable Long grupoId,
            @RequestParam(required = false) String palavraChave,
            @RequestParam(required = false) String organizacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        EditalPaginadoResponse response = editalService.listar(
                grupoId, palavraChave, organizacao, dataInicio, dataFim, page, size, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EditalResponse> buscar(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(editalService.buscar(id, grupoId, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EditalResponse> atualizar(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            @Valid @RequestBody EditarEditalRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(editalService.atualizar(id, grupoId, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            Authentication authentication) {
        editalService.removerDoGrupo(id, grupoId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
