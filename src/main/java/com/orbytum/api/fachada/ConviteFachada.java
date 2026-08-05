package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.EnviarConviteRequest;
import com.orbytum.api.models.dto.response.ConviteEnviadoResponse;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.service.ConviteService;
import com.orbytum.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConviteFachada {

    private final ConviteService conviteService;
    private final UsuarioService usuarioService;

    public ConviteEnviadoResponse enviarConvite(EnviarConviteRequest request, String emailUsuarioLogado) {
        Usuario remetente = usuarioService.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));

        return conviteService.enviarConvite(remetente, request.grupoId(), request.emailConvidado());
    }
}
