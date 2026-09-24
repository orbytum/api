package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.AtualizarStatusAtividadeRequest;
import com.orbytum.api.models.dto.request.CreateAtividadeRequest;
import com.orbytum.api.models.dto.request.EditAtividadeRequest;
import com.orbytum.api.models.dto.response.AtividadeResponse;
import com.orbytum.api.service.AtividadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/atividades")
@RequiredArgsConstructor
public class AtividadeController {

    private final AtividadeService atividadeService;

    @PostMapping
    public ResponseEntity<AtividadeResponse> criarAtividade(
            @Valid @RequestBody CreateAtividadeRequest request,
            Authentication authentication) {
        AtividadeResponse response = atividadeService.criar(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtividadeResponse> atualizarAtividade(
            @PathVariable Long id,
            @Valid @RequestBody EditAtividadeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(atividadeService.atualizar(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AtividadeResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusAtividadeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(atividadeService.atualizarStatus(id, request.status(), authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAtividade(
            @PathVariable Long id,
            Authentication authentication) {
        atividadeService.deletar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtividadeResponse> buscarAtividade(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(atividadeService.buscar(id, authentication.getName()));
    }

    @GetMapping("/projeto/{projetoId}")
    public ResponseEntity<List<AtividadeResponse>> listarPorProjeto(
            @PathVariable Long projetoId,
            Authentication authentication) {
        return ResponseEntity.ok(atividadeService.listarPorProjeto(projetoId, authentication.getName()));
    }

    @GetMapping("/projeto/{projetoId}/atrasadas")
    public ResponseEntity<List<AtividadeResponse>> listarAtrasadas(
            @PathVariable Long projetoId,
            Authentication authentication) {
        return ResponseEntity.ok(atividadeService.listarAtrasadasPorProjeto(projetoId, authentication.getName()));
    }
}
