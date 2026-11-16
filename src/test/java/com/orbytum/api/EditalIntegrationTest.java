package com.orbytum.api;

import com.orbytum.api.models.dto.request.CriarEditalRequest;
import com.orbytum.api.models.dto.response.EditalPaginadoResponse;
import com.orbytum.api.models.dto.response.EditalResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.exceptions.EditalNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.EditalRepository;
import com.orbytum.api.repository.EditalXGrupoRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.EditalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EditalIntegrationTest {

    @Autowired
    private EditalService editalService;

    @Autowired
    private EditalRepository editalRepository;

    @Autowired
    private EditalXGrupoRepository editalXGrupoRepository;

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
    private ProjetoRepository projetoRepository;

    @Autowired
    private ProjetoXUsuarioRepository projetoXUsuarioRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String LIDER_EMAIL = "edital.lider@teste.com";
    private static final String PESQUISADOR_EMAIL = "edital.pesquisador@teste.com";
    private static final String ESTRANGEIRO_EMAIL = "edital.estrangeiro@teste.com";

    private Grupo grupo;
    private Projeto projeto;
    private Role role;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            role = roleRepository.save(new Role("Membro Edital", List.of(), false));

            Usuario lider = usuarioRepository.save(new Usuario("Líder Edital", LIDER_EMAIL, "11977770001", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    lider.getEmail(), "senha123", AccessLevel.USER, lider, null));

            Usuario pesquisador = usuarioRepository.save(new Usuario("Pesquisador Edital", PESQUISADOR_EMAIL, "11977770002", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    pesquisador.getEmail(), "senha123", AccessLevel.USER, pesquisador, null));

            Usuario estrangeiro = usuarioRepository.save(new Usuario("Estrangeiro Edital", ESTRANGEIRO_EMAIL, "11977770003", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    estrangeiro.getEmail(), "senha123", AccessLevel.USER, estrangeiro, null));

            grupo = grupoRepository.save(new Grupo("Grupo Editais IT", lider));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, lider, role, NivelMembro.LIDER, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, pesquisador, role, NivelMembro.PESQUISADOR, null, true));

            projeto = projetoRepository.save(new Projeto(
                    grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto Editais", "Assunto"));
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            editalXGrupoRepository.deleteAll();
            editalRepository.deleteAll();
            projetoXUsuarioRepository.deleteAll();
            projetoRepository.deleteAll();
            grupoXUsuarioRepository.deleteAll();
            grupoRepository.deleteAll();
            credenciaisLoginRepository.deleteAll();
            usuarioRepository.deleteAll();
            if (role != null) {
                roleRepository.deleteById(role.getId());
            }
        });
    }

    private EditalResponse criar(String organizador, String descricao, String uri,
                                 LocalDateTime abertura, LocalDateTime encerramento, String email) {
        return editalService.criar(grupo.getId(),
                new CriarEditalRequest(organizador, descricao, null, uri, abertura, encerramento, null), email);
    }

    @Test
    void liderCriaEditalELista() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Edital de tecnologia", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);

        EditalPaginadoResponse listagem = editalService.listar(
                grupo.getId(), null, null, null, null, null, false, 1, 10, PESQUISADOR_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("CNPq", listagem.items().get(0).organizador());
    }

    @Test
    void pesquisadorNaoPodeCriarEdital() {
        LocalDateTime agora = LocalDateTime.now();
        assertThrows(SemPermissaoNoGrupoErro.class, () -> criar(
                "FAPESP", "Edital de biologia", "https://b.com/2", agora, agora.plusDays(10), PESQUISADOR_EMAIL));
    }

    @Test
    void filtroPorPalavraChave() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Edital de tecnologia e inovação", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);
        criar("FAPESP", "Edital de biologia marinha", "https://b.com/2", agora, agora.plusDays(20), LIDER_EMAIL);

        EditalPaginadoResponse listagem = editalService.listar(
                grupo.getId(), null, "tecnologia", null, null, null, false, 1, 10, LIDER_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("CNPq", listagem.items().get(0).organizador());
    }

    @Test
    void filtroPorProjetoRetornaEditalRotulado() {
        LocalDateTime agora = LocalDateTime.now();
        editalService.criar(grupo.getId(),
                new CriarEditalRequest("CNPq", "Edital do projeto", null, "https://a.com/1",
                        agora, agora.plusDays(10), List.of(projeto.getId())),
                LIDER_EMAIL);
        editalService.criar(grupo.getId(),
                new CriarEditalRequest("FAPESP", "Edital sem rótulo", null, "https://b.com/2",
                        agora, agora.plusDays(20), null),
                LIDER_EMAIL);

        EditalPaginadoResponse listagem = editalService.listar(
                grupo.getId(), projeto.getId(), null, null, null, null, false, 1, 10, LIDER_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("CNPq", listagem.items().get(0).organizador());
        assertEquals(1, listagem.items().get(0).projetos().size());
        assertEquals(projeto.getId(), listagem.items().get(0).projetos().get(0).id());
    }

    @Test
    void filtroPorOrganizacao() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Edital A", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);
        criar("FAPESP", "Edital B", "https://b.com/2", agora, agora.plusDays(20), LIDER_EMAIL);

        EditalPaginadoResponse listagem = editalService.listar(
                grupo.getId(), null, null, "fapesp", null, null, false, 1, 10, LIDER_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("FAPESP", listagem.items().get(0).organizador());
    }

    @Test
    void filtroPorPeriodo() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Edital A", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);
        criar("FAPESP", "Edital B", "https://b.com/2", agora, agora.plusDays(60), LIDER_EMAIL);

        LocalDate inicio = agora.toLocalDate();
        LocalDate fim = agora.toLocalDate().plusDays(30);
        EditalPaginadoResponse listagem = editalService.listar(
                grupo.getId(), null, null, null, inicio, fim, false, 1, 10, LIDER_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals("CNPq", listagem.items().get(0).organizador());
    }

    @Test
    void membroDeOutroGrupoNaoConsulta() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Edital A", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);

        assertThrows(RuntimeException.class, () -> editalService.listar(
                grupo.getId(), null, null, null, null, null, false, 1, 10, ESTRANGEIRO_EMAIL));
    }

    // ---------------------------------------------------------------- REQ-060: palavras-chave do grupo

    @Test
    void liderCadastraPalavrasChaveNormalizadasESemDuplicatas() {
        List<String> salvas = editalService.atualizarPalavrasChave(
                grupo.getId(), List.of("  IA aplicada ", "ia aplicada", "", "agricultura"), LIDER_EMAIL);

        assertEquals(List.of("IA aplicada", "agricultura"), salvas);
        assertEquals(salvas, editalService.listarPalavrasChave(grupo.getId(), PESQUISADOR_EMAIL));
    }

    @Test
    void pesquisadorNaoGerenciaPalavrasChave() {
        assertThrows(SemPermissaoNoGrupoErro.class, () ->
                editalService.atualizarPalavrasChave(grupo.getId(), List.of("tema"), PESQUISADOR_EMAIL));
    }

    @Test
    void palavraChaveComMaisDe60CaracteresEhRejeitada() {
        assertThrows(IllegalArgumentException.class, () ->
                editalService.atualizarPalavrasChave(grupo.getId(), List.of("x".repeat(61)), LIDER_EMAIL));
    }

    @Test
    void filtroDeRelevanciaUsaDescricaoEOrganizacao() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Chamada de IA aplicada à saúde", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);
        criar("Embrapa Agricultura", "Programa de inovação", "https://a.com/2", agora, agora.plusDays(10), LIDER_EMAIL);
        criar("CAPES", "Bolsas de física teórica", "https://a.com/3", agora, agora.plusDays(10), LIDER_EMAIL);
        editalService.atualizarPalavrasChave(grupo.getId(), List.of("ia aplicada", "agricultura"), LIDER_EMAIL);

        EditalPaginadoResponse todos = editalService.listar(
                grupo.getId(), null, null, null, null, null, false, 1, 10, PESQUISADOR_EMAIL);
        EditalPaginadoResponse relevantes = editalService.listar(
                grupo.getId(), null, null, null, null, null, true, 1, 10, PESQUISADOR_EMAIL);

        assertEquals(3, todos.totalElements());
        assertEquals(2, relevantes.totalElements());
        assertTrue(relevantes.items().stream().noneMatch(e -> e.organizador().equals("CAPES")));
    }

    @Test
    void filtroDeRelevanciaSemPalavrasChaveNaoRetornaEditais() {
        LocalDateTime agora = LocalDateTime.now();
        criar("CNPq", "Edital qualquer", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);

        EditalPaginadoResponse relevantes = editalService.listar(
                grupo.getId(), null, null, null, null, null, true, 1, 10, LIDER_EMAIL);

        assertEquals(0, relevantes.totalElements());
    }

    // ---------------------------------------------------------------- REQ-093: visibilidade por projeto

    private EditalResponse criarDoProjeto(String organizador, String uri) {
        LocalDateTime agora = LocalDateTime.now();
        return editalService.criar(grupo.getId(), new CriarEditalRequest(
                organizador, "Edital restrito", null, uri, agora, agora.plusDays(10), List.of(projeto.getId())),
                LIDER_EMAIL);
    }

    @Test
    void editalDeProjetoNaoApareceParaQuemNaoParticipa() {
        LocalDateTime agora = LocalDateTime.now();
        criar("Público", "Edital aberto", "https://a.com/1", agora, agora.plusDays(10), LIDER_EMAIL);
        EditalResponse restrito = criarDoProjeto("Restrito", "https://a.com/2");

        EditalPaginadoResponse doPesquisador = editalService.listar(
                grupo.getId(), null, null, null, null, null, false, 1, 10, PESQUISADOR_EMAIL);
        EditalPaginadoResponse doLider = editalService.listar(
                grupo.getId(), null, null, null, null, null, false, 1, 10, LIDER_EMAIL);

        assertEquals(1, doPesquisador.totalElements());
        assertEquals("Público", doPesquisador.items().get(0).organizador());
        assertEquals(2, doLider.totalElements());
        assertThrows(EditalNaoEncontradoErro.class, () ->
                editalService.buscar(restrito.id(), grupo.getId(), PESQUISADOR_EMAIL));
    }

    @Test
    void participanteDoProjetoVeEditalDoProjeto() {
        EditalResponse restrito = criarDoProjeto("Restrito", "https://a.com/2");
        transactionTemplate.executeWithoutResult(status -> projetoXUsuarioRepository.save(new ProjetoXUsuario(
                projeto, usuarioRepository.findByEmail(PESQUISADOR_EMAIL).orElseThrow(), NivelMembro.PESQUISADOR)));

        EditalPaginadoResponse doPesquisador = editalService.listar(
                grupo.getId(), null, null, null, null, null, false, 1, 10, PESQUISADOR_EMAIL);

        assertEquals(1, doPesquisador.totalElements());
        assertEquals(restrito.id(), editalService.buscar(restrito.id(), grupo.getId(), PESQUISADOR_EMAIL).id());
    }
}
