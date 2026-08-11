package com.orbytum.api.configuration;

import com.orbytum.api.model.entity.CredenciaisLogin;
import com.orbytum.api.model.entity.Usuario;
import com.orbytum.api.model.enums.AccessLevel;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class   AdminInicialInitializerTest {

    @Mock
    private CredenciaisLoginService credenciaisLoginService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInicialInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "adminInicialEmail", "admin.inicial@orbytum.com");
        ReflectionTestUtils.setField(initializer, "adminInicialPassword", "AdminInit@123");
    }

    @Test
    void run_QuandoNaoExisteAdmin_CriaAdminInicial() {
        when(credenciaisLoginService.existsByAccessLevel(AccessLevel.ADMIN)).thenReturn(false);
        when(credenciaisLoginService.existsByAccessLevel(AccessLevel.INITIAL_ADMIN)).thenReturn(false);
        when(usuarioService.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("AdminInit@123")).thenReturn("encodedPassword");

        initializer.run();

        verify(usuarioService, times(1)).save(any(Usuario.class));
        verify(credenciaisLoginService, times(1)).save(any(CredenciaisLogin.class));
    }

    @Test
    void run_QuandoJaExisteAdminDefinitivo_NaoCriaAdminInicial() {
        when(credenciaisLoginService.existsByAccessLevel(AccessLevel.ADMIN)).thenReturn(true);

        initializer.run();

        verify(usuarioService, never()).save(any(Usuario.class));
        verify(credenciaisLoginService, never()).save(any(CredenciaisLogin.class));
    }

    @Test
    void run_QuandoJaExisteAdminInicial_NaoCriaAdminInicialDuplicado() {
        when(credenciaisLoginService.existsByAccessLevel(AccessLevel.ADMIN)).thenReturn(false);
        when(credenciaisLoginService.existsByAccessLevel(AccessLevel.INITIAL_ADMIN)).thenReturn(true);

        initializer.run();

        verify(usuarioService, never()).save(any(Usuario.class));
        verify(credenciaisLoginService, never()).save(any(CredenciaisLogin.class));
    }
}
