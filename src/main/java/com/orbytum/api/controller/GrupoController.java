package com.orbytum.api.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbytum.api.service.GrupoService;
import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.request.CreateLeaderRequest;
import com.orbytum.api.models.dto.request.EditGroupRequest;
import com.orbytum.api.models.dto.request.EditLeaderRequest;
import com.orbytum.api.models.dto.response.GrupoPaginadoResponse;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.dto.response.LiderResponse;
import com.orbytum.api.models.dto.response.MeuGrupoResponse;
import com.orbytum.api.models.dto.response.PesquisadorResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<GrupoResponse> criarGrupo(@Valid @RequestBody CreateGroupRequest request) {
        GrupoResponse response = grupoService.criarGrupo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<GrupoResponse> atualizarGrupo(
            @PathVariable Long id,
            @Valid @RequestBody EditGroupRequest request) {
        GrupoResponse response = grupoService.atualizarGrupo(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<GrupoPaginadoResponse> listarGrupos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String usuario) {
        GrupoPaginadoResponse response = grupoService.listarGrupos(page, size, nome, usuario);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<GrupoResponse> buscarGrupoPorId(@PathVariable Long id) {
        GrupoResponse response = grupoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<Void> removerGrupo(@PathVariable Long id) {
        grupoService.removerGrupo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{grupoId}/lideres")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<LiderResponse> cadastrarLider(
            @PathVariable Long grupoId,
            @Valid @RequestBody CreateLeaderRequest request) {
        LiderResponse response = grupoService.cadastrarLider(grupoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{grupoId}/lideres/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<LiderResponse> atualizarLider(
            @PathVariable Long grupoId,
            @PathVariable Long usuarioId,
            @Valid @RequestBody EditLeaderRequest request) {
        LiderResponse response = grupoService.atualizarLider(grupoId, usuarioId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{grupoId}/pesquisadores")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<List<PesquisadorResponse>> listarPesquisadores(@PathVariable Long grupoId) {
        List<PesquisadorResponse> response = grupoService.listarPesquisadores(grupoId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{grupoId}/pesquisadores/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<PesquisadorResponse> atualizarPesquisador(
            @PathVariable Long grupoId,
            @PathVariable Long usuarioId,
            @Valid @RequestBody EditLeaderRequest request) {
        PesquisadorResponse response = grupoService.atualizarPesquisador(grupoId, usuarioId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{grupoId}/pesquisadores/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<Void> removerPesquisador(
            @PathVariable Long grupoId,
            @PathVariable Long usuarioId) {
        grupoService.removerPesquisador(grupoId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/meus-grupos")
    public ResponseEntity<List<MeuGrupoResponse>> listarMeusGrupos(Authentication authentication) {
        String emailUsuarioLogado = authentication.getName();
        List<MeuGrupoResponse> response = grupoService.listarMeusGrupos(emailUsuarioLogado);
        return ResponseEntity.ok(response);
    }
}