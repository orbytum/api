package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.EditUsuarioRequest;
import com.orbytum.api.models.dto.response.UsuarioPaginadoResponse;
import com.orbytum.api.models.dto.response.UsuarioResponse;
import com.orbytum.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<UsuarioPaginadoResponse> listarUsuarios(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(usuarioService.listar(page, size, busca));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<UsuarioResponse> atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody EditUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<Void> inativarUsuario(@PathVariable Long id) {
        usuarioService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
