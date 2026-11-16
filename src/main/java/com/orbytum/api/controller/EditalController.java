package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CriarEditalRequest;
import com.orbytum.api.models.dto.request.EditarEditalRequest;
import com.orbytum.api.models.dto.response.DocumentoResponse;
import com.orbytum.api.models.dto.response.EditalPaginadoResponse;
import com.orbytum.api.models.dto.response.EditalResponse;
import com.orbytum.api.service.ArquivoDownload;
import com.orbytum.api.service.EditalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

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
            @RequestParam(required = false) Long projetoId,
            @RequestParam(required = false) String palavraChave,
            @RequestParam(required = false) String organizacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "false") boolean somenteRelevantes,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        EditalPaginadoResponse response = editalService.listar(
                grupoId, projetoId, palavraChave, organizacao, dataInicio, dataFim, somenteRelevantes,
                page, size, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /** REQ-060: palavras-chave dos temas do grupo. */
    @GetMapping("/palavras-chave")
    public ResponseEntity<List<String>> listarPalavrasChave(
            @PathVariable Long grupoId,
            Authentication authentication) {
        return ResponseEntity.ok(editalService.listarPalavrasChave(grupoId, authentication.getName()));
    }

    @PutMapping("/palavras-chave")
    public ResponseEntity<List<String>> atualizarPalavrasChave(
            @PathVariable Long grupoId,
            @RequestBody List<String> palavras,
            Authentication authentication) {
        return ResponseEntity.ok(editalService.atualizarPalavrasChave(grupoId, palavras, authentication.getName()));
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

    @GetMapping("/{id}/documentos")
    public ResponseEntity<List<DocumentoResponse>> listarDocumentos(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(editalService.listarDocumentos(id, grupoId, authentication.getName()));
    }

    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> anexarDocumento(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo,
            Authentication authentication) {
        DocumentoResponse response = editalService.anexarDocumento(id, grupoId, arquivo, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/documentos/{documentoId}")
    public ResponseEntity<DocumentoResponse> vincularDocumento(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            @PathVariable Long documentoId,
            Authentication authentication) {
        DocumentoResponse response = editalService.vincularDocumento(
                id, grupoId, documentoId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/documentos/{documentoId}")
    public ResponseEntity<Void> removerDocumento(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            @PathVariable Long documentoId,
            Authentication authentication) {
        editalService.removerDocumento(id, grupoId, documentoId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/documentos/{documentoId}/arquivo")
    public ResponseEntity<byte[]> baixarDocumento(
            @PathVariable Long grupoId,
            @PathVariable Long id,
            @PathVariable Long documentoId,
            Authentication authentication) {
        ArquivoDownload download = editalService.baixarDocumento(
                id, grupoId, documentoId, authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.nomeArquivo().replace("\"", "") + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.bytes());
    }
}
