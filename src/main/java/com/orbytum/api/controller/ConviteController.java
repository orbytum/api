package com.orbytum.api.controller;

import com.orbytum.api.service.ConviteService;
import com.orbytum.api.models.dto.request.EnviarConviteRequest;
import com.orbytum.api.models.dto.request.GerarConviteCadastroRequest;
import com.orbytum.api.models.dto.request.GerarConviteGrupoRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroDetalheResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroPaginadoResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoDetalheResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoResponse;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convites")
@RequiredArgsConstructor
public class ConviteController {

    private final ConviteService conviteService;

    @PostMapping
    public ResponseEntity<ConviteGrupoEnviadoResponse> enviarConvite(
            @Valid @RequestBody EnviarConviteRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteGrupoEnviadoResponse response = conviteService.enviarConvite(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/grupo")
    public ResponseEntity<ConviteGrupoResponse> gerarConviteGrupo(
            @Valid @RequestBody GerarConviteGrupoRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteGrupoResponse response = conviteService.gerarConviteGrupo(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/aceitar/grupo/{token}", "/grupo/{token}"})
    public ResponseEntity<ConviteGrupoDetalheResponse> buscarConviteGrupo(
            @PathVariable String token
    ) {
        ConviteGrupoDetalheResponse response = conviteService.buscarConviteGrupoPorToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/aceitar/grupo/{token}")
    public ResponseEntity<ConviteGrupoEnviadoResponse> aceitarConviteGrupo(
            @PathVariable String token,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteGrupoEnviadoResponse response = conviteService.aceitarConviteGrupo(token, emailUsuarioLogado);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cadastro")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<ConviteCadastroResponse> gerarConviteCadastro(
            @Valid @RequestBody GerarConviteCadastroRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteCadastroResponse response = conviteService.gerarConviteCadastro(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"", "/cadastro"})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<ConviteCadastroPaginadoResponse> listarConvitesCadastro(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "ativos") String status,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteCadastroPaginadoResponse response = conviteService.listarConvitesCadastro(
                emailUsuarioLogado,
                page,
                size,
                email,
                status
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cadastro/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'admin', 'ROLE_ADMIN')")
    public ResponseEntity<Void> revogarConviteCadastro(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        conviteService.revogarConviteCadastro(id, emailUsuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/aceitar/cadastro/{token}")
    public ResponseEntity<AuthResponse> aceitarConviteCadastro(
            @PathVariable String token,
            @RequestBody RegisterRequest request
    ) {
        AuthResponse response = conviteService.aceitarConviteCadastro(token, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/aceitar/grupo/{token}/cadastro")
    public ResponseEntity<AuthResponse> aceitarConviteGrupoCadastro(
            @PathVariable String token,
            @RequestBody RegisterRequest request
    ) {
        AuthResponse response = conviteService.aceitarConviteGrupoCadastro(token, request);
        return ResponseEntity.ok(response);
    }
}
