package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CriarSolicitacaoRequest;
import com.orbytum.api.models.dto.request.EditarSolicitacaoRequest;
import com.orbytum.api.models.dto.response.SolicitacaoResponse;
import com.orbytum.api.service.SolicitacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criar(
            @Valid @RequestBody CriarSolicitacaoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitacaoService.criar(request, authentication.getName()));
    }

    @GetMapping("/projeto/{projetoId}")
    public ResponseEntity<List<SolicitacaoResponse>> listarPorProjeto(
            @PathVariable Long projetoId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitacaoService.listarPorProjeto(projetoId, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitacaoService.buscarPorId(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EditarSolicitacaoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitacaoService.atualizar(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @PathVariable Long id,
            Authentication authentication
    ) {
        solicitacaoService.remover(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<SolicitacaoResponse> aprovar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitacaoService.aprovar(id, authentication.getName()));
    }

    @PatchMapping("/{id}/rejeitar")
    public ResponseEntity<SolicitacaoResponse> rejeitar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitacaoService.rejeitar(id, authentication.getName()));
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<SolicitacaoResponse> concluir(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(solicitacaoService.concluir(id, authentication.getName()));
    }
}
