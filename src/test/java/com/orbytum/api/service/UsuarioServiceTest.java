package com.orbytum.api.service;

import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String EMAIL = "pesquisador@test.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CredenciaisLoginRepository credenciaisLoginRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario() {
        Usuario usuario = new Usuario("Pesquisador", EMAIL, "41999990000", "Dr");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    @Test
    void inativarDesativaUsuarioELogin() {
        Usuario usuario = usuario();
        CredenciaisLogin credenciais = new CredenciaisLogin(EMAIL, "hash", AccessLevel.USER, usuario, null);
        when(credenciaisLoginRepository.findByEmail(EMAIL)).thenReturn(Optional.of(credenciais));

        usuarioService.inativar(1L);

        assertFalse(usuario.isAtivo());
        assertFalse(credenciais.isAtivo());
    }

    @Test
    void naoInativaAdministrador() {
        Usuario usuario = usuario();
        CredenciaisLogin credenciais = new CredenciaisLogin(EMAIL, "hash", AccessLevel.ADMIN, usuario, null);
        when(credenciaisLoginRepository.findByEmail(EMAIL)).thenReturn(Optional.of(credenciais));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.inativar(1L));
        assertTrue(usuario.isAtivo());
        assertTrue(credenciais.isAtivo());
        verify(usuarioRepository, never()).save(any());
    }
}
