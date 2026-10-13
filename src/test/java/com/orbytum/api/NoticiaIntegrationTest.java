package com.orbytum.api;

import com.orbytum.api.models.dto.request.CreateNoticiaRequest;
import com.orbytum.api.models.dto.response.NoticiaPaginadaResponse;
import com.orbytum.api.models.dto.response.NoticiaResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.enums.NoticiaStatus;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.NoticiaRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.NoticiaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class NoticiaIntegrationTest {

    @Autowired
    private NoticiaService noticiaService;

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CredenciaisLoginRepository credenciaisLoginRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String LIDER_EMAIL = "noticia.lider@teste.com";
    private static final String PESQUISADOR_EMAIL = "noticia.pesquisador@teste.com";
    private static final String ESTRANGEIRO_EMAIL = "noticia.estrangeiro@teste.com";

    private Grupo grupo;
    private Usuario lider;
    private Usuario pesquisador;
    private Role role;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            role = roleRepository.save(new Role("Membro", List.of(), false));

            lider = usuarioRepository.save(new Usuario("Líder Notícia", LIDER_EMAIL, "11966660001", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    lider.getEmail(), "senha123", AccessLevel.USER, lider, null));

            pesquisador = usuarioRepository.save(new Usuario("Pesquisador Notícia", PESQUISADOR_EMAIL, "11966660002", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    pesquisador.getEmail(), "senha123", AccessLevel.USER, pesquisador, null));

            Usuario estrangeiro = usuarioRepository.save(new Usuario("Estrangeiro Notícia", ESTRANGEIRO_EMAIL, "11966660003", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    estrangeiro.getEmail(), "senha123", AccessLevel.USER, estrangeiro, null));

            grupo = grupoRepository.save(new Grupo("Grupo Notícias IT", lider));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, lider, role, NivelMembro.COORDENADOR, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, pesquisador, role, NivelMembro.PESQUISADOR, null, true));
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            noticiaRepository.deleteAll();
            grupoXUsuarioRepository.deleteAll();
            grupoRepository.deleteAll();
            credenciaisLoginRepository.deleteAll();
            usuarioRepository.deleteAll();
            if (role != null) {
                roleRepository.deleteById(role.getId());
            }
        });
    }

    private NoticiaResponse criarComoPesquisador(String titulo, LocalDateTime dataFim) {
        return noticiaService.criar(
                new CreateNoticiaRequest(grupo.getId(), titulo, "Conteúdo da notícia", "https://ref.com", "Tecnologia", null, dataFim),
                PESQUISADOR_EMAIL);
    }

    @Test
    void noticiaDePesquisadorNascePendenteENaoApareceNaListagem() {
        NoticiaResponse criada = criarComoPesquisador("Descoberta", null);

        assertEquals(NoticiaStatus.PENDENTE, criada.status());

        NoticiaPaginadaResponse listagem = noticiaService.listar(grupo.getId(), null, null, null, 1, 10, PESQUISADOR_EMAIL);

        assertEquals(0, listagem.totalElements());
    }

    @Test
    void noticiaAprovadaPorCoordenadorApareceNaListagem() {
        NoticiaResponse criada = criarComoPesquisador("Descoberta", null);

        NoticiaResponse aprovada = noticiaService.aprovar(criada.id(), LIDER_EMAIL);
        assertEquals(NoticiaStatus.APROVADA, aprovada.status());
        assertNotNull(aprovada.dataFimExibicao());

        NoticiaPaginadaResponse listagem = noticiaService.listar(grupo.getId(), null, null, null, 1, 10, PESQUISADOR_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("Descoberta", listagem.items().get(0).titulo());
    }

    @Test
    void listaPendentesRetornaSomentePendentesDoGrupo() {
        criarComoPesquisador("Pendente 1", null);
        NoticiaResponse segunda = criarComoPesquisador("Pendente 2", null);
        noticiaService.aprovar(segunda.id(), LIDER_EMAIL);

        NoticiaPaginadaResponse pendentes = noticiaService.listarPendentes(grupo.getId(), 1, 10, LIDER_EMAIL);

        assertEquals(1, pendentes.totalElements());
        assertEquals("Pendente 1", pendentes.items().get(0).titulo());
    }

    @Test
    void pesquisadorNaoPodeAprovar() {
        NoticiaResponse criada = criarComoPesquisador("Descoberta", null);

        assertThrows(SemPermissaoNoGrupoErro.class, () -> noticiaService.aprovar(criada.id(), PESQUISADOR_EMAIL));
    }

    @Test
    void noticiaExpiradaNaoApareceNaListagem() {
        NoticiaResponse criada = criarComoPesquisador("Antiga", LocalDateTime.now().minusDays(1));
        noticiaService.aprovar(criada.id(), LIDER_EMAIL);

        NoticiaPaginadaResponse listagem = noticiaService.listar(grupo.getId(), null, null, null, 1, 10, LIDER_EMAIL);

        assertEquals(0, listagem.totalElements());
    }

    @Test
    void filtroPorAssuntoRetornaApenasCorrespondentes() {
        NoticiaResponse tecnologia = criarComoPesquisador("Tech", null);
        NoticiaResponse biologia = criarComoPesquisador("Bio", null);
        noticiaService.atualizar(biologia.id(),
                new com.orbytum.api.models.dto.request.EditNoticiaRequest(
                        biologia.titulo(), biologia.conteudo(), biologia.referencia(), "Biologia", null, null),
                PESQUISADOR_EMAIL);
        noticiaService.aprovar(tecnologia.id(), LIDER_EMAIL);
        noticiaService.aprovar(biologia.id(), LIDER_EMAIL);

        NoticiaPaginadaResponse listagem = noticiaService.listar(grupo.getId(), "Tecnologia", null, null, 1, 10, LIDER_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("Tech", listagem.items().get(0).titulo());
    }

    @Test
    void membroDeOutroGrupoNaoConsultaNoticias() {
        criarComoPesquisador("Descoberta", null);

        assertThrows(RuntimeException.class,
                () -> noticiaService.listar(grupo.getId(), null, null, null, 1, 10, ESTRANGEIRO_EMAIL));
    }
}
