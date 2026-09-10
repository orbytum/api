package com.orbytum.api.controller;

import com.orbytum.api.fachada.ProjetoFachada;
import com.orbytum.api.models.dto.request.CreateProjetoRequest;
import com.orbytum.api.models.dto.request.EditProjetoRequest;
import com.orbytum.api.models.dto.response.ProjetoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<ProjetoResponse> criarProjeto(@Valid @RequestBody CreateProjetoRequest request) {
        ProjetoResponse response = projetoFachada.criarProjeto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<ProjetoResponse> atualizarProjeto(
            @PathVariable Long id,
            @Valid @RequestBody EditProjetoRequest request) {
        ProjetoResponse response = projetoFachada.atualizarProjeto(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<Void> deletarProjeto(@PathVariable Long id) {
        projetoFachada.deletarProjeto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProjetoResponse>> listarProjetos() {
        List<ProjetoResponse> projetos = projetoFachada.listarProjetos();
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponse> buscarProjeto(@PathVariable Long id) {
        ProjetoResponse response = projetoFachada.buscarProjeto(id);
        return ResponseEntity.ok(response);
    }
}
