package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.request.EditLembreteRequest;
import com.orbytum.api.models.dto.response.LembretePaginadoResponse;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.service.LembreteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/lembretes")
@RequiredArgsConstructor
public class LembreteController {

    private final LembreteService lembreteService;

    @PostMapping
    public ResponseEntity<LembreteResponse> criarLembrete(
            @Valid @RequestBody CreateLembreteRequest request,
            Authentication authentication) {
        LembreteResponse response = lembreteService.criarLembrete(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<LembretePaginadoResponse> listarLembretes(
            @RequestParam(required = false) Long grupoId,
            @RequestParam(required = false) TipoLembrete tipo,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        LembretePaginadoResponse response = lembreteService.listarLembretes(grupoId, tipo, busca, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LembreteResponse> buscarPorId(@PathVariable Long id) {
        LembreteResponse response = lembreteService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LembreteResponse> atualizarLembrete(
            @PathVariable Long id,
            @Valid @RequestBody EditLembreteRequest request,
            Authentication authentication) {
        LembreteResponse response = lembreteService.atualizarLembrete(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarLembrete(
            @PathVariable Long id,
            Authentication authentication) {
        lembreteService.inativarLembrete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/ata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LembreteResponse> anexarAta(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        LembreteResponse response = lembreteService.anexarAtaReuniao(id, file, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/ata")
    public ResponseEntity<LembreteResponse> removerAta(
            @PathVariable Long id,
            Authentication authentication) {
        LembreteResponse response = lembreteService.removerAtaReuniao(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/ata/download")
    public ResponseEntity<byte[]> baixarAta(@PathVariable Long id) {
        byte[] data = lembreteService.baixarAta(id);
        LembreteResponse lembrete = lembreteService.buscarPorId(id);
        String filename = lembrete.ataNomeOriginal() != null ? lembrete.ataNomeOriginal() : "ata.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
