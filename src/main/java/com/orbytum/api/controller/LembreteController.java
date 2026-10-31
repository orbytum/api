package com.orbytum.api.controller;

import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.request.EditLembreteRequest;
import com.orbytum.api.models.dto.response.LembretePaginadoResponse;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.service.LembreteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping({"/lembretes", "/eventos"})
@RequiredArgsConstructor
@Tag(name = "Lembretes", description = "Endpoints para gerenciamento de lembretes, recorrências e atas de reunião")
public class LembreteController {

    private final LembreteService lembreteService;

    @PostMapping
    @Operation(summary = "Criar um novo lembrete vinculado a um grupo de pesquisa")
    public ResponseEntity<LembreteResponse> criarLembrete(
            @Valid @RequestBody CreateLembreteRequest request,
            Authentication authentication) {
        LembreteResponse response = lembreteService.criar(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lembrete por ID")
    public ResponseEntity<LembreteResponse> buscarLembrete(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.buscar(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar lembrete (somente o criador do lembrete)")
    public ResponseEntity<LembreteResponse> atualizarLembrete(
            @PathVariable Long id,
            @Valid @RequestBody EditLembreteRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.atualizar(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir lembrete (inativação lógica)")
    public ResponseEntity<Void> deletarLembrete(
            @PathVariable Long id,
            Authentication authentication) {
        lembreteService.deletar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar lembretes com filtros e paginação")
    public ResponseEntity<LembretePaginadoResponse> listarLembretes(
            @RequestParam Long grupoId,
            @RequestParam(required = false) TipoLembrete tipo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.listarPorGrupo(grupoId, tipo, busca, dataInicio, dataFim, page, size, authentication.getName()));
    }

    @GetMapping("/grupo/{grupoId}")
    @Operation(summary = "Listar lembretes de um grupo específico com filtros e paginação")
    public ResponseEntity<LembretePaginadoResponse> listarPorGrupo(
            @PathVariable Long grupoId,
            @RequestParam(required = false) TipoLembrete tipo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.listarPorGrupo(grupoId, tipo, busca, dataInicio, dataFim, page, size, authentication.getName()));
    }

    @GetMapping("/grupo/{grupoId}/todos")
    @Operation(summary = "Listar todos os lembretes ativos de um grupo (sem paginação), ordenados pela próxima ocorrência, com filtros opcionais")
    public ResponseEntity<List<LembreteResponse>> listarTodosPorGrupo(
            @PathVariable Long grupoId,
            @RequestParam(required = false) TipoLembrete tipo,
            @RequestParam(required = false) String busca,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.listarTodosPorGrupo(grupoId, tipo, busca, authentication.getName()));
    }

    @GetMapping(value = "/grupo/{grupoId}/ics", produces = "text/calendar")
    @Operation(summary = "Exportar os lembretes do grupo em arquivo iCalendar (.ics)")
    public ResponseEntity<byte[]> exportarIcs(
            @PathVariable Long grupoId,
            Authentication authentication) {
        byte[] arquivo = lembreteService.exportarIcs(grupoId, authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"lembretes-grupo-" + grupoId + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(arquivo);
    }

    @PostMapping(value = "/{id}/ata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Inserir arquivo de ata de reunião (apenas tipo Reunião e somente o criador)")
    public ResponseEntity<LembreteResponse> inserirAta(
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo,
            Authentication authentication) {
        LembreteResponse response = lembreteService.inserirAta(id, arquivo, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/ata")
    @Operation(summary = "Baixar ou visualizar arquivo de ata de reunião")
    public ResponseEntity<byte[]> downloadAta(
            @PathVariable Long id,
            Authentication authentication) {
        LembreteResponse lembrete = lembreteService.buscar(id, authentication.getName());
        byte[] bytes = lembreteService.downloadAta(id, authentication.getName());

        String filename = lembrete.ataNomeArquivo() != null ? lembrete.ataNomeArquivo() : "ata.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PatchMapping("/{id}/presenca")
    @Operation(summary = "Alternar ou confirmar presença no lembrete")
    public ResponseEntity<LembreteResponse> alternarPresenca(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.confirmarPresenca(id, authentication.getName()));
    }

    @PostMapping("/{id}/confirmar-presenca")
    @Operation(summary = "Confirmar presença no lembrete")
    public ResponseEntity<LembreteResponse> confirmarPresenca(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(lembreteService.confirmarPresenca(id, authentication.getName()));
    }
}
