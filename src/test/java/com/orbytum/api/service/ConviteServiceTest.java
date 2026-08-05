package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.GerarLinkConviteRequest;
import com.orbytum.api.models.dto.response.ConviteEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteLinkResponse;
import com.orbytum.api.models.entity.Convite;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.exceptions.*;
import com.orbytum.api.repository.ConviteRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConviteServiceTest {

    @Mock
    private ConviteRepository conviteRepository;

    @Mock
    private GrupoService grupoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoXUsuarioService grupoXUsuarioService;

    @Mock
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private ConviteService conviteService;

    private Usuario remetente;
    private Usuario convidado;
    private Grupo grupoAberto;
    private Grupo grupoRestrito;
    private Role roleComPermissao;
    private Role roleSemPermissao;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        remetente = new Usuario("Lider Teste", "lider@test.com", "12345678", "Dev");
        convidado = new Usuario("Convidado Teste", "convidado@test.com", "87654321", "Dev");
        grupoAberto = new Grupo("Grupo Aberto", true, TipoIngressoGrupo.ABERTO_VIA_CONVITE);
        grupoRestrito = new Grupo("Grupo Restrito", true, TipoIngressoGrupo.RESTRITO);

        roleComPermissao = new Role(1L, "Lider", List.of(Permissao.PROJETO_CONVITE_CRIAR));
        roleSemPermissao = new Role(2L, "Membro", List.of());

        projeto = Projeto.builder()
                .id(10L)
                .grupo(grupoAberto)
                .titulo("Projeto Teste")
                .assunto("Assunto")
                .status(ProjetoStatus.EM_ANDAMENTO)
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();
    }

    @Test
    void enviarConvite_SucessoComProjetos() {
        Long grupoId = 1L;
        GrupoXUsuario gxu = new GrupoXUsuario(grupoAberto, remetente, roleComPermissao, true);

        when(grupoService.findById(grupoId)).thenReturn(Optional.of(grupoAberto));
        when(grupoXUsuarioService.findByGrupoIdAndUsuarioId(grupoId, remetente.getId())).thenReturn(Optional.of(gxu));
        when(usuarioService.findByEmail("convidado@test.com")).thenReturn(Optional.of(convidado));
        when(grupoXUsuarioService.isUsuarioNoGrupo(grupoId, convidado.getId())).thenReturn(false);
        when(projetoService.findAllByIds(List.of(10L))).thenReturn(List.of(projeto));
        when(conviteRepository.save(any(Convite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConviteEnviadoResponse response = conviteService.enviarConvite(remetente, grupoId, "convidado@test.com", List.of(10L));

        assertNotNull(response);
        assertEquals("Grupo Aberto", response.nomeGrupo());
        assertEquals("convidado@test.com", response.emailConvidado());
        assertEquals(List.of(10L), response.projetoIds());
        assertTrue(response.isAtivo());
        verify(conviteRepository, times(1)).save(any(Convite.class));
    }

    @Test
    void gerarLinkConvite_Sucesso() {
        Long grupoId = 1L;
        GrupoXUsuario gxu = new GrupoXUsuario(grupoAberto, remetente, roleComPermissao, true);

        when(grupoService.findById(grupoId)).thenReturn(Optional.of(grupoAberto));
        when(grupoXUsuarioService.findByGrupoIdAndUsuarioId(grupoId, remetente.getId())).thenReturn(Optional.of(gxu));
        when(conviteRepository.save(any(Convite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GerarLinkConviteRequest req = new GerarLinkConviteRequest(grupoId, null, 7);
        ConviteLinkResponse response = conviteService.gerarLinkConvite(remetente, req);

        assertNotNull(response);
        assertNotNull(response.token());
        assertTrue(response.urlConvite().contains(response.token()));
        assertEquals("Grupo Aberto", response.nomeGrupo());
        verify(conviteRepository, times(1)).save(any(Convite.class));
    }

    @Test
    void gerarLinkConvite_GrupoRestrito_LancaExcecao() {
        Long grupoId = 2L;
        GrupoXUsuario gxu = new GrupoXUsuario(grupoRestrito, remetente, roleComPermissao, true);

        when(grupoService.findById(grupoId)).thenReturn(Optional.of(grupoRestrito));
        when(grupoXUsuarioService.findByGrupoIdAndUsuarioId(grupoId, remetente.getId())).thenReturn(Optional.of(gxu));

        GerarLinkConviteRequest req = new GerarLinkConviteRequest(grupoId, null, 7);
        assertThrows(IngressoRestritoErro.class, () -> conviteService.gerarLinkConvite(remetente, req));
    }

    @Test
    void aceitarConvitePorLink_Sucesso() {
        String token = "uuid-valido";
        Convite convite = new Convite(grupoAberto, remetente, token, List.of(), LocalDateTime.now().plusDays(5));

        when(conviteRepository.findByTokenAndIsAtivoTrue(token)).thenReturn(Optional.of(convite));
        when(grupoXUsuarioService.isUsuarioNoGrupo(grupoAberto.getId(), convidado.getId())).thenReturn(false);
        when(roleRepository.findByNome("Membro")).thenReturn(Optional.of(roleSemPermissao));

        ConviteEnviadoResponse response = conviteService.aceitarConvitePorLink(token, convidado);

        assertNotNull(response);
        assertEquals("Grupo Aberto", response.nomeGrupo());
        assertEquals(convidado.getEmail(), response.emailConvidado());
        verify(grupoXUsuarioRepository, times(1)).save(any(GrupoXUsuario.class));
    }

    @Test
    void aceitarConvitePorLink_LinkExpirado_LancaExcecao() {
        String token = "uuid-expirado";
        Convite convite = new Convite(grupoAberto, remetente, token, List.of(), LocalDateTime.now().minusDays(1));

        when(conviteRepository.findByTokenAndIsAtivoTrue(token)).thenReturn(Optional.of(convite));

        assertThrows(ConviteInvalidoOuExpiradoErro.class, () -> conviteService.aceitarConvitePorLink(token, convidado));
    }
}
