package com.orbytum.api.controller;

import com.orbytum.api.fachada.GrupoFachada;
import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.response.GrupoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoFachada grupoFachada;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')") //validando role admin
    public ResponseEntity<GrupoResponse> criarGrupo(@Valid @RequestBody CreateGroupRequest request) {
        GrupoResponse response = grupoFachada.criarGrupo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GrupoResponse>> listarGrupos() {
        List<GrupoResponse> grupos = grupoFachada.listarGrupos();
        return ResponseEntity.ok(grupos);
    }
}