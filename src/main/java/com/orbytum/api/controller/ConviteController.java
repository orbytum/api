package com.orbytum.api.controller;

import com.orbytum.api.fachada.ConviteFachada;
import com.orbytum.api.models.dto.request.EnviarConviteRequest;
import com.orbytum.api.models.dto.request.GerarLinkConviteRequest;
import com.orbytum.api.models.dto.response.ConviteEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteLinkResponse;
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
    public ResponseEntity<ConviteEnviadoResponse> enviarConvite(
            @Valid @RequestBody EnviarConviteRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteEnviadoResponse response = conviteFachada.enviarConvite(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/gerar-link")
    public ResponseEntity<ConviteLinkResponse> gerarLinkConvite(
            @Valid @RequestBody GerarLinkConviteRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteLinkResponse response = conviteFachada.gerarLinkConvite(request, emailUsuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/aceitar-link/{token}")
    public ResponseEntity<ConviteEnviadoResponse> aceitarConvitePorLink(
            @PathVariable String token,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication.getName();
        ConviteEnviadoResponse response = conviteFachada.aceitarConvitePorLink(token, emailUsuarioLogado);
        return ResponseEntity.ok(response);
    }
}
