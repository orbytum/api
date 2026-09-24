package com.orbytum.api.service;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrupoAcessoServiceTest {

    @Mock
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    @Mock
    private ProjetoXUsuarioRepository projetoXUsuarioRepository;

    @InjectMocks
    private GrupoAcessoService grupoAcessoService;

    private GrupoXUsuario vinculo(NivelMembro nivel) {
        Grupo grupo = new Grupo("Grupo", true);
        grupo.setId(1L);
        Usuario usuario = new Usuario();
        Role role = new Role("Membro", List.of(), nivel == NivelMembro.LIDER);
        return new GrupoXUsuario(grupo, usuario, role, nivel, null, true);
    }

    @Test
    void deveIdentificarLider() {
        when(grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(1L, 10L))
                .thenReturn(Optional.of(vinculo(NivelMembro.LIDER)));

        assertTrue(grupoAcessoService.isLider(1L, 10L));
        assertTrue(grupoAcessoService.isCoordenadorOuAcima(1L, 10L));
    }

    @Test
    void pesquisadorNaoEhCoordenador() {
        when(grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(1L, 10L))
                .thenReturn(Optional.of(vinculo(NivelMembro.PESQUISADOR)));

        assertFalse(grupoAcessoService.isLider(1L, 10L));
        assertFalse(grupoAcessoService.isCoordenadorOuAcima(1L, 10L));
    }

    @Test
    void deveLancarErroQuandoNaoEhMembro() {
        when(grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(1L, 99L))
                .thenReturn(Optional.empty());

        assertThrows(SemPermissaoNoGrupoErro.class,
                () -> grupoAcessoService.validarMembro(1L, 99L));
    }
}
