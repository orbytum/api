package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CriarMaterialRequest;
import com.orbytum.api.models.dto.request.EditarMaterialRequest;
import com.orbytum.api.models.dto.response.MaterialResponse;
import com.orbytum.api.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(materialService.listar(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> buscarPorId(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(materialService.buscarPorId(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> criar(
            @Valid @RequestBody CriarMaterialRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.criar(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EditarMaterialRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(materialService.atualizar(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @PathVariable Long id,
            Authentication authentication) {
        materialService.remover(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
