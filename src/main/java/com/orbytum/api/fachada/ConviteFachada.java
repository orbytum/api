package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.EnviarConviteRequest;
import com.orbytum.api.models.dto.request.GerarConviteCadastroRequest;
import com.orbytum.api.models.dto.request.GerarConviteGrupoRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.EmailJaCadastradoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.service.ConviteService;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.UsuarioService;
import com.orbytum.api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ConviteFachada {

    private final ConviteService conviteService;
    private final UsuarioService usuarioService;

    public ConviteGrupoEnviadoResponse enviarConvite(EnviarConviteRequest request, String emailLogado) {
        Usuario remetente = usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
        return conviteService.enviarConviteGrupo(
                remetente,
                request.idGrupo(),
                request.email(),
                request.idsProjeto()
        );
    }

    public ConviteGrupoResponse gerarConviteGrupo(GerarConviteGrupoRequest request, String emailLogado) {
        Usuario remetente = usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
        return conviteService.gerarConviteGrupo(remetente, request);
    }

    public ConviteCadastroResponse gerarConviteCadastro(GerarConviteCadastroRequest request, String emailLogado) {
        Usuario remetente = usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
        return conviteService.gerarConviteCadastro(remetente, request);
    }

    public ConviteGrupoEnviadoResponse aceitarConviteGrupo(String token, String emailLogado) {
        Usuario user = usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
        return conviteService.aceitarConviteGrupo(token, user);
    }

    public AuthResponse aceitarConviteCadastro(String token, RegisterRequest request) {
        return conviteService.aceitarConviteCadastro(token, request);
    }

}
