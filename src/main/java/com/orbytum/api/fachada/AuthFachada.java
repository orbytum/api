package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.request.LoginRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.models.exceptions.ContaDesativadaErro;
import com.orbytum.api.models.exceptions.CrenciaisInvalidas;
import com.orbytum.api.models.exceptions.EmailJaCadastradoErro;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.GrupoXUsuarioService;
import com.orbytum.api.service.UsuarioService;
import com.orbytum.api.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthFachada {

    private final CredenciaisLoginService credenciaisLoginService;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioService grupoXUsuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        CredenciaisLogin credenciais = credenciaisLoginService
                .findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!credenciais.isAtivo()) {
            throw new ContaDesativadaErro("Conta desativada");
        }

        if (!passwordEncoder.matches(request.senha(), credenciais.getSenha())) {
            throw new CrenciaisInvalidas("Credenciais inválidas");
        }

        List<Permissao> permissoes = grupoXUsuarioService
                .findPermissoesByUsuario(credenciais.getUsuario());

        UserDetails userDetails = User.builder()
                .username(credenciais.getEmail())
                .password(credenciais.getSenha())
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtil.generateToken(userDetails, credenciais.getAccessLevel(), permissoes);
        return new AuthResponse(token, "Bearer");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (credenciaisLoginService.existsByEmail(request.email())) {
            throw new EmailJaCadastradoErro("Email já cadastrado");
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                request.telefone(),
                request.titulo()
        );

        usuario = usuarioService.save(usuario);

        AccessLevel accessLevel = usuarioService.count() == 1
                ? AccessLevel.ADMIN
                : AccessLevel.USER;

        CredenciaisLogin credenciais = new CredenciaisLogin(
                request.email(),
                passwordEncoder.encode(request.senha()),
                accessLevel,
                usuario,
                null
        );
        credenciaisLoginService.save(credenciais);

        UserDetails userDetails = User.builder()
                .username(request.email())
                .password(credenciais.getSenha())
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtil.generateToken(userDetails, accessLevel, Collections.emptyList());
        return new AuthResponse(token, "Bearer");
    }
}
