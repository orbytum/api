package com.orbytum.api.controller;

import com.orbytum.api.fachada.ProjetoFachada;
import com.orbytum.api.models.dto.request.AdicionarParticipanteRequest;
import com.orbytum.api.models.dto.request.CreateProjetoRequest;
import com.orbytum.api.models.dto.request.EditProjetoRequest;
import com.orbytum.api.models.dto.response.ParticipanteResponse;
import com.orbytum.api.models.dto.response.ProjetoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoFachada projetoFachada;

    @PostMapping
    public ResponseEntity<ProjetoResponse> criarProjeto(@Valid @RequestBody CreateProjetoRequest request) {
        ProjetoResponse response = projetoFachada.criarProjeto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponse> atualizarProjeto(
            @PathVariable Long id,
            @Valid @RequestBody EditProjetoRequest request) {
        ProjetoResponse response = projetoFachada.atualizarProjeto(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarProjeto(@PathVariable Long id) {
        projetoFachada.deletarProjeto(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/favorito")
    public ResponseEntity<ProjetoResponse> alternarFavorito(@PathVariable Long id) {
        return ResponseEntity.ok(projetoFachada.alternarFavorito(id));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ProjetoResponse> finalizarProjeto(@PathVariable Long id) {
        return ResponseEntity.ok(projetoFachada.finalizarProjeto(id));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ProjetoResponse>> listarProjetos(@PathVariable Long grupoId) {
        List<ProjetoResponse> projetos = projetoFachada.listarProjetosPorGrupo(grupoId);
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponse> buscarProjeto(@PathVariable Long id) {
        ProjetoResponse response = projetoFachada.buscarProjeto(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/participantes")
    public ResponseEntity<List<ParticipanteResponse>> listarParticipantes(@PathVariable Long id) {
        return ResponseEntity.ok(projetoFachada.listarParticipantes(id));
    }

    @PostMapping("/{id}/participantes")
    public ResponseEntity<ParticipanteResponse> adicionarParticipante(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarParticipanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoFachada.adicionarParticipante(id, request));
    }

    @DeleteMapping("/{id}/participantes/{usuarioId}")
    public ResponseEntity<Void> removerParticipante(
            @PathVariable Long id,
            @PathVariable Long usuarioId) {
        projetoFachada.removerParticipante(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
