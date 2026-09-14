package com.orbytum.api.service;

import com.orbytum.api.models.dto.response.MeuGrupoResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrupoServiceTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private GrupoXUsuarioService grupoXUsuarioService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private CredenciaisLoginService credenciaisLoginService;

    @Mock
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private GrupoService grupoService;

    @Test
    void testListarMeusGruposParaUsuarioComum() {
        String email = "user@test.com";
        CredenciaisLogin credenciais = mock(CredenciaisLogin.class);
        when(credenciais.getAccessLevel()).thenReturn(AccessLevel.USER);
        when(credenciaisLoginService.findByEmail(email)).thenReturn(Optional.of(credenciais));

        Grupo grupo = new Grupo("Grupo Alfa", true);
        grupo.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setNome("João");
        Role role = new Role("Líder", Collections.emptyList(), true);
        GrupoXUsuario vinculo = new GrupoXUsuario(grupo, usuario, role, true);

        when(grupoXUsuarioRepository.findAllByUsuarioEmailAndIsAtivoTrueAndGrupoIsAtivoTrue(email))
                .thenReturn(List.of(vinculo));

        List<MeuGrupoResponse> responses = grupoService.listarMeusGrupos(email);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("Grupo Alfa", responses.get(0).nome());
        assertEquals("Líder", responses.get(0).role());
        assertTrue(responses.get(0).isLider());
    }

    @Test
    void testListarMeusGruposParaAdmin() {
        String email = "admin@test.com";
        CredenciaisLogin credenciais = mock(CredenciaisLogin.class);
        when(credenciais.getAccessLevel()).thenReturn(AccessLevel.ADMIN);
        when(credenciaisLoginService.findByEmail(email)).thenReturn(Optional.of(credenciais));

        Grupo grupo = new Grupo("Grupo Global", true);
        grupo.setId(2L);
        when(grupoRepository.findAllByIsAtivoTrue()).thenReturn(List.of(grupo));

        List<MeuGrupoResponse> responses = grupoService.listarMeusGrupos(email);

        assertEquals(1, responses.size());
        assertEquals(2L, responses.get(0).id());
        assertEquals("Grupo Global", responses.get(0).nome());
        assertEquals("Administrador", responses.get(0).role());
        assertTrue(responses.get(0).isLider());
    }
}
