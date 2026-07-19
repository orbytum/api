package com.orbytum.api.service;

import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.request.LoginRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import com.orbytum.api.util.JwtUtil;
import com.orbytum.api.models.exceptions.ContaDesativadaErro;
import com.orbytum.api.models.exceptions.CrenciaisInvalidas;
import com.orbytum.api.models.exceptions.EmailJaCadastradoErro;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredenciaisLoginRepository credenciaisLoginRepository;
    private final UsuarioRepository usuarioRepository;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        CredenciaisLogin credenciais = credenciaisLoginRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Credenciais invÃ¡lidas"));

        if (!credenciais.isAtivo()) {
            throw new ContaDesativadaErro("Conta desativada");
        }

        if (!passwordEncoder.matches(request.senha(), credenciais.getSenha())) {
            throw new CrenciaisInvalidas("Credenciais invÃ¡lidas");
        }

        List<Permissao> permissoes = grupoXUsuarioRepository
                .findPermissoesByUsuario(credenciais.getUsuario());

        UserDetails userDetails = User.builder()
                .username(credenciais.getEmail())
                .password(credenciais.getSenha())
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtil.generateToken(userDetails, credenciais.getAccessLevel(), permissoes);
        return new AuthResponse(token, "Bearer");
    }

    public AuthResponse register(RegisterRequest request) {
        if (credenciaisLoginRepository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoErro("Email jÃ¡ cadastrado");
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                request.telefone(),
                request.titulo()
        );
        usuario = usuarioRepository.save(usuario);

        AccessLevel accessLevel = usuarioRepository.count() == 1
                ? AccessLevel.ADMIN
                : AccessLevel.USER;

        CredenciaisLogin credenciais = new CredenciaisLogin(
                request.email(),
                passwordEncoder.encode(request.senha()),
                accessLevel,
                usuario,
                null
        );
        credenciaisLoginRepository.save(credenciais);

        UserDetails userDetails = User.builder()
                .username(request.email())
                .password(credenciais.getSenha())
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtil.generateToken(userDetails, accessLevel, Collections.emptyList());
        return new AuthResponse(token, "Bearer");
    }
}
