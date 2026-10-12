package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateNoticiaRequest;
import com.orbytum.api.models.dto.request.EditNoticiaRequest;
import com.orbytum.api.models.dto.request.RejeitarNoticiaRequest;
import com.orbytum.api.models.dto.response.NoticiaPaginadaResponse;
import com.orbytum.api.models.dto.response.NoticiaResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Noticia;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.NoticiaStatus;
import com.orbytum.api.models.exceptions.NoticiaNaoPendenteErro;
import com.orbytum.api.repository.NoticiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticiaServiceTest {

    private static final String EMAIL = "coordenador@test.com";

    @Mock
    private NoticiaRepository noticiaRepository;

    @Mock
    private GrupoService grupoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoAcessoService grupoAcessoService;

    @InjectMocks
    private NoticiaService noticiaService;

    private Grupo grupo;
    private Usuario autor;
    private Usuario coordenador;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noticiaService, "prazoExibicaoDias", 30);

        grupo = new Grupo("Grupo Notícias", true);
        grupo.setId(1L);

        autor = usuario(10L, "Autor Pesquisador", "autor@test.com", AccessLevel.USER);
        coordenador = usuario(20L, "Coordenador", EMAIL, AccessLevel.USER);
    }

    private Usuario usuario(Long id, String nome, String email, AccessLevel nivel) {
        CredenciaisLogin credenciais = new CredenciaisLogin(email, "hash", nivel, new Usuario(), null);
        return new Usuario(id, nome, email, "11999999999", "Dr", LocalDateTime.now(), true, credenciais);
    }

    private Noticia noticia(NoticiaStatus status, Usuario autorNoticia, LocalDateTime dataFim) {
        Noticia noticia = new Noticia(grupo, autorNoticia, "Título", "Conteúdo", "https://ref.com", "Tecnologia", null, dataFim);
        noticia.setId(1L);
        noticia.setStatus(status);
        return noticia;
    }

    @Test
    void criarNoticiaNascePendenteComAutorLogado() {
        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(noticiaRepository.save(any(Noticia.class))).thenAnswer(inv -> inv.getArgument(0));

        NoticiaResponse response = noticiaService.criar(
                new CreateNoticiaRequest(1L, "Novo título", "Novo conteúdo", "https://ref.com", "Ciência", null, null),
                EMAIL);

        assertEquals(NoticiaStatus.PENDENTE, response.status());
        assertEquals(20L, response.autorId());
        assertEquals(1L, response.grupoId());
        assertTrue(response.isAtivo());
    }

    @Test
    void aprovarPorCoordenadorDefineStatusEPrazoPadrao() {
        when(noticiaRepository.findByIdAndIsAtivoTrue(1L)).thenReturn(Optional.of(noticia(NoticiaStatus.PENDENTE, autor, null)));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(noticiaRepository.save(any(Noticia.class))).thenAnswer(inv -> inv.getArgument(0));

        NoticiaResponse response = noticiaService.aprovar(1L, EMAIL);

        assertEquals(NoticiaStatus.APROVADA, response.status());
        assertNotNull(response.dataFimExibicao());
        assertEquals(20L, response.aprovadorId());
        assertNotNull(response.dthAprovacao());
        verify(grupoAcessoService).validarCoordenadorOuAcima(1L, 20L);
    }

    @Test
    void aprovarNoticiaJaAprovadaLancaErro() {
        when(noticiaRepository.findByIdAndIsAtivoTrue(1L))
                .thenReturn(Optional.of(noticia(NoticiaStatus.APROVADA, autor, LocalDateTime.now().plusDays(5))));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));

        assertThrows(NoticiaNaoPendenteErro.class, () -> noticiaService.aprovar(1L, EMAIL));
    }

    @Test
    void rejeitarPendenteGravaMotivo() {
        when(noticiaRepository.findByIdAndIsAtivoTrue(1L)).thenReturn(Optional.of(noticia(NoticiaStatus.PENDENTE, autor, null)));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(noticiaRepository.save(any(Noticia.class))).thenAnswer(inv -> inv.getArgument(0));

        NoticiaResponse response = noticiaService.rejeitar(1L, new RejeitarNoticiaRequest("Falta referência"), EMAIL);

        assertEquals(NoticiaStatus.REJEITADA, response.status());
        assertEquals("Falta referência", response.motivoRejeicao());
    }

    @Test
    void autorNaoPodeEditarNoticiaAprovada() {
        when(noticiaRepository.findByIdAndIsAtivoTrue(1L))
                .thenReturn(Optional.of(noticia(NoticiaStatus.APROVADA, autor, LocalDateTime.now().plusDays(5))));
        when(usuarioService.findByEmail("autor@test.com")).thenReturn(Optional.of(autor));
        when(grupoAcessoService.isCoordenadorOuAcima(1L, 10L)).thenReturn(false);

        EditNoticiaRequest request =
                new EditNoticiaRequest("Título editado", "Conteúdo editado", null, "Assunto", null, null);

        assertThrows(NoticiaNaoPendenteErro.class, () -> noticiaService.atualizar(1L, request, "autor@test.com"));
    }

    @Test
    void editarNoticiaRejeitadaVoltaParaPendente() {
        Noticia rejeitada = noticia(NoticiaStatus.REJEITADA, autor, null);
        rejeitada.setMotivoRejeicao("Motivo antigo");
        when(noticiaRepository.findByIdAndIsAtivoTrue(1L)).thenReturn(Optional.of(rejeitada));
        when(usuarioService.findByEmail("autor@test.com")).thenReturn(Optional.of(autor));
        when(grupoAcessoService.isCoordenadorOuAcima(1L, 10L)).thenReturn(false);
        when(noticiaRepository.save(any(Noticia.class))).thenAnswer(inv -> inv.getArgument(0));

        EditNoticiaRequest request =
                new EditNoticiaRequest("Título novo", "Conteúdo novo", "https://nova.com", "Assunto", null, null);

        NoticiaResponse response = noticiaService.atualizar(1L, request, "autor@test.com");

        assertEquals(NoticiaStatus.PENDENTE, response.status());
        assertNull(response.motivoRejeicao());
    }

    @Test
    void listarConsultaApenasAprovadasVigentes() {
        Noticia aprovada = noticia(NoticiaStatus.APROVADA, autor, LocalDateTime.now().plusDays(10));
        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(noticiaRepository.filtrarVigentes(eq(1L), eq(NoticiaStatus.APROVADA), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(aprovada)));

        NoticiaPaginadaResponse response = noticiaService.listar(1L, "  Tecnologia  ", null, null, 1, 10, EMAIL);

        assertEquals(1, response.items().size());
        assertEquals(NoticiaStatus.APROVADA, response.items().get(0).status());
        verify(noticiaRepository).filtrarVigentes(eq(1L), eq(NoticiaStatus.APROVADA), any(), eq("Tecnologia"), any(), any(), any());
    }

    @Test
    void periodDeExibicaoInvalidoLancaErro() {
        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));

        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = inicio.minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> noticiaService.criar(
                new CreateNoticiaRequest(1L, "T", "C", null, "A", inicio, fim), EMAIL));
    }
}
