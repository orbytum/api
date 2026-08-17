package com.orbytum.api.controller;

import com.orbytum.api.fachada.ConviteFachada;
import com.orbytum.api.models.dto.request.EnviarConviteRequest;
import com.orbytum.api.models.dto.request.GerarConviteCadastroRequest;
import com.orbytum.api.models.dto.request.GerarConviteGrupoRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convites")
@RequiredArgsConstructor
public class ConviteController {

    private final ConviteFachada conviteFachada;

    @PostMapping
    public ResponseEntity<ConviteGrupoEnviadoResponse> enviarConvite(
            @Valid @RequestBody EnviarConviteRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteGrupoEnviadoResponse response = conviteFachada.enviarConvite(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/grupo")
    public ResponseEntity<ConviteGrupoResponse> gerarConviteGrupo(
            @Valid @RequestBody GerarConviteGrupoRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteGrupoResponse response = conviteFachada.gerarConviteGrupo(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/aceitar/grupo/{token}")
    public ResponseEntity<ConviteGrupoEnviadoResponse> aceitarConviteGrupo(
            @PathVariable String token,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteGrupoEnviadoResponse response = conviteFachada.aceitarConviteGrupo(token, emailUsuarioLogado);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<ConviteCadastroResponse> gerarConviteCadastro(
            @Valid @RequestBody GerarConviteCadastroRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteCadastroResponse response = conviteFachada.gerarConviteCadastro(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/aceitar/cadastro/{token}")
    public ResponseEntity<AuthResponse> aceitarConviteGrupo(
            @PathVariable String token,
            @RequestBody RegisterRequest request
    ) {
        AuthResponse response = conviteFachada.aceitarConviteCadastro(token, request);
        return ResponseEntity.ok(response);
    }
}
