package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.RegisterAdminRequest;
import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.GrupoXUsuarioService;
import com.orbytum.api.service.UsuarioService;
import com.orbytum.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthFachadaTest {

    @Mock
    private CredenciaisLoginService credenciaisLoginService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoXUsuarioService grupoXUsuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthFachada authFachada;

    private Usuario usuarioInicial;
    private CredenciaisLogin credenciaisInicial;

    @BeforeEach
    void setUp() {
        usuarioInicial = new Usuario("Admin Inicial", "admin.inicial@orbytum.com", "000", "Init");
        credenciaisInicial = new CredenciaisLogin("admin.inicial@orbytum.com", "pass", AccessLevel.INITIAL_ADMIN, usuarioInicial, null);
    }

    @Test
    void registerAdmin_PeloAdminInicial_CriaAdminEDeletaAdminInicial() {
        RegisterAdminRequest req = new RegisterAdminRequest(
                "Admin Definitivo",
                "admin.definitivo@orbytum.com",
                "Senha@123",
                "11999999999",
                "Administrador"
        );

        when(credenciaisLoginService.findByEmail("admin.inicial@orbytum.com")).thenReturn(Optional.of(credenciaisInicial));
        when(credenciaisLoginService.existsByEmail("admin.definitivo@orbytum.com")).thenReturn(false);
        when(usuarioService.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("Senha@123")).thenReturn("encoded");
        when(credenciaisLoginService.findByAccessLevel(AccessLevel.INITIAL_ADMIN)).thenReturn(Optional.of(credenciaisInicial));
        when(jwtUtil.generateToken(any(), eq(AccessLevel.ADMIN), any())).thenReturn("token-admin");

        AuthResponse response = authFachada.registerAdmin(req, "admin.inicial@orbytum.com");

        assertNotNull(response);
        assertEquals("token-admin", response.token());

        verify(credenciaisLoginService, times(1)).delete(credenciaisInicial);
        verify(usuarioService, times(1)).delete(usuarioInicial);
    }

    @Test
    void registerAdmin_UsuarioSemPermissao_LancaExcecao() {
        Usuario usuarioComum = new Usuario("User", "user@test.com", "111", "Dev");
        CredenciaisLogin credenciaisUser = new CredenciaisLogin("user@test.com", "pass", AccessLevel.USER, usuarioComum, null);

        RegisterAdminRequest req = new RegisterAdminRequest("Admin", "admin@test.com", "pass", "111", "Dir");
        when(credenciaisLoginService.findByEmail("user@test.com")).thenReturn(Optional.of(credenciaisUser));

        assertThrows(AccessDeniedException.class, () -> authFachada.registerAdmin(req, "user@test.com"));
        verify(credenciaisLoginService, never()).delete(any());
    }
}
