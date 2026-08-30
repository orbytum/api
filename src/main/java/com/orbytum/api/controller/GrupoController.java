package com.orbytum.api.controller;

import java.util.List;

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

import com.orbytum.api.fachada.GrupoFachada;
import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.request.CreateLeaderRequest;
import com.orbytum.api.models.dto.request.EditGroupRequest;
import com.orbytum.api.models.dto.request.EditLeaderRequest;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.dto.response.LiderResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoFachada grupoFachada;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<GrupoResponse> criarGrupo(@Valid @RequestBody CreateGroupRequest request) {
        GrupoResponse response = grupoFachada.criarGrupo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<GrupoResponse> atualizarGrupo(
            @PathVariable Long id,
            @Valid @RequestBody EditGroupRequest request) {
        GrupoResponse response = grupoFachada.atualizarGrupo(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<List<GrupoResponse>> listarGrupos() {
        List<GrupoResponse> grupos = grupoFachada.listarGrupos();
        return ResponseEntity.ok(grupos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<GrupoResponse> buscarGrupoPorId(@PathVariable Long id) {
        GrupoResponse response = grupoFachada.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<Void> removerGrupo(@PathVariable Long id) {
        grupoFachada.removerGrupo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{grupoId}/lideres")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<LiderResponse> cadastrarLider(
            @PathVariable Long grupoId,
            @Valid @RequestBody CreateLeaderRequest request) {
        LiderResponse response = grupoFachada.cadastrarLider(grupoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{grupoId}/lideres/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'INITIAL_ADMIN')")
    public ResponseEntity<LiderResponse> atualizarLider(
            @PathVariable Long grupoId,
            @PathVariable Long usuarioId,
            @Valid @RequestBody EditLeaderRequest request) {
        LiderResponse response = grupoFachada.atualizarLider(grupoId, usuarioId, request);
        return ResponseEntity.ok(response);
    }
}