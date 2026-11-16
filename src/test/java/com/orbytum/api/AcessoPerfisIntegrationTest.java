package com.orbytum.api;

import com.orbytum.api.fachada.ProjetoFachada;
import com.orbytum.api.models.dto.request.CreateAtividadeRequest;
import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.request.CriarSolicitacaoRequest;
import com.orbytum.api.models.dto.request.GerarConviteGrupoRequest;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.dto.response.ProjetoResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.exceptions.SemPermissaoConvidarErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.AtividadeRepository;
import com.orbytum.api.repository.ConviteGrupoRepository;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.EventoParticipanteRepository;
import com.orbytum.api.repository.EventoRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.SolicitacaoRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.AtividadeService;
import com.orbytum.api.service.ConviteService;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.LembreteService;
import com.orbytum.api.service.SolicitacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regras de acesso por perfil: convites só pelo administrador, leitura de projeto restrita a quem tem acesso,
 * membros do grupo visíveis só ao líder do grupo e colaborador somente leitura.
 */
@SpringBootTest
class AcessoPerfisIntegrationTest {

    private static final String LIDER = "acesso.lider@teste.com";
    private static final String PESQUISADOR = "acesso.pesquisador@teste.com";
    private static final String COLABORADOR = "acesso.colaborador@teste.com";
    private static final String DE_FORA = "acesso.foradoprojeto@teste.com";

    @Autowired private ProjetoFachada projetoFachada;
    @Autowired private AtividadeService atividadeService;
    @Autowired private ConviteService conviteService;
    @Autowired private GrupoService grupoService;
    @Autowired private LembreteService lembreteService;
    @Autowired private SolicitacaoService solicitacaoService;

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CredenciaisLoginRepository credenciaisLoginRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private GrupoRepository grupoRepository;
    @Autowired private GrupoXUsuarioRepository grupoXUsuarioRepository;
    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private ProjetoXUsuarioRepository projetoXUsuarioRepository;
    @Autowired private AtividadeRepository atividadeRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private EventoParticipanteRepository eventoParticipanteRepository;
    @Autowired private SolicitacaoRepository solicitacaoRepository;
    @Autowired private ConviteGrupoRepository conviteGrupoRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Grupo grupo;
    private Projeto projetoA;
    private Projeto projetoB;
    private Role role;

    private Usuario usuario(String nome, String email, String telefone) {
        Usuario u = usuarioRepository.save(new Usuario(nome, email, telefone, "Ms"));
        credenciaisLoginRepository.save(new CredenciaisLogin(email, "senha", AccessLevel.USER, u, null));
        return u;
    }

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            role = roleRepository.save(new Role("Membro Acesso", List.of(), false));
            Usuario lider = usuario("Líder", LIDER, "11944440001");
            Usuario pesquisador = usuario("Pesquisador", PESQUISADOR, "11944440002");
            Usuario colaborador = usuario("Colaborador", COLABORADOR, "11944440003");
            Usuario deFora = usuario("De fora", DE_FORA, "11944440004");

            grupo = grupoRepository.save(new Grupo("Grupo Acesso Perfis", lider));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, lider, role, NivelMembro.LIDER, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, pesquisador, role, NivelMembro.PESQUISADOR, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, colaborador, role, NivelMembro.COLABORADOR, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, deFora, role, NivelMembro.PESQUISADOR, null, true));

            projetoA = projetoRepository.save(new Projeto(grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto A", "Assunto"));
            projetoB = projetoRepository.save(new Projeto(grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto B", "Assunto"));
            projetoXUsuarioRepository.save(new ProjetoXUsuario(projetoA, pesquisador, NivelMembro.PESQUISADOR));
            projetoXUsuarioRepository.save(new ProjetoXUsuario(projetoA, colaborador, NivelMembro.COLABORADOR));
            projetoXUsuarioRepository.save(new ProjetoXUsuario(projetoB, deFora, NivelMembro.PESQUISADOR));
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transactionTemplate.executeWithoutResult(status -> {
            eventoParticipanteRepository.deleteAll();
            eventoRepository.deleteAll();
            solicitacaoRepository.deleteAll();
            atividadeRepository.deleteAll();
            conviteGrupoRepository.deleteAll();
            projetoXUsuarioRepository.deleteAll();
            projetoRepository.deleteAll();
            grupoXUsuarioRepository.deleteAll();
            grupoRepository.deleteAll();
            credenciaisLoginRepository.deleteAll();
            usuarioRepository.deleteAll();
            roleRepository.deleteById(role.getId());
        });
    }

    private void logarComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    // ---------------------------------------------------------------- convites

    @Test
    void liderDoGrupoNaoAdicionaMembros() {
        assertThrows(SemPermissaoConvidarErro.class, () -> conviteService.gerarConviteGrupo(
                new GerarConviteGrupoRequest(grupo.getId(), List.of(), 5, 1, null, NivelMembro.PESQUISADOR), LIDER));
    }

    // ---------------------------------------------------------------- leitura de projetos

    @Test
    void cadaIntegranteVeApenasOsProjetosDeQueParticipa() {
        logarComo(PESQUISADOR);
        List<String> doPesquisador = projetoFachada.listarProjetosPorGrupo(grupo.getId()).stream()
                .map(ProjetoResponse::titulo).toList();
        logarComo(LIDER);
        List<String> doLider = projetoFachada.listarProjetosPorGrupo(grupo.getId()).stream()
                .map(ProjetoResponse::titulo).toList();

        assertEquals(List.of("Projeto A"), doPesquisador);
        assertTrue(doLider.containsAll(List.of("Projeto A", "Projeto B")));
    }

    @Test
    void projetoEParticipantesSoParaQuemTemAcesso() {
        logarComo(DE_FORA);
        assertThrows(SemPermissaoNoGrupoErro.class, () -> projetoFachada.buscarProjeto(projetoA.getId()));
        assertThrows(SemPermissaoNoGrupoErro.class, () -> projetoFachada.listarParticipantes(projetoA.getId()));

        logarComo(COLABORADOR);
        assertEquals(projetoA.getId(), projetoFachada.buscarProjeto(projetoA.getId()).id());
        assertEquals(2, projetoFachada.listarParticipantes(projetoA.getId()).size());
    }

    @Test
    void atividadesSoDeProjetosAcessiveis() {
        atividadeService.criar(new CreateAtividadeRequest(projetoB.getId(),
                usuarioRepository.findByEmail(DE_FORA).orElseThrow().getId(), null, "Tarefa B", "Descrição",
                LocalDateTime.now().plusDays(3)), LIDER);

        assertTrue(atividadeService.listarPorGrupo(grupo.getId(), PESQUISADOR).isEmpty());
        assertEquals(1, atividadeService.listarPorGrupo(grupo.getId(), LIDER).size());
        assertThrows(SemPermissaoNoGrupoErro.class,
                () -> atividadeService.listarPorProjeto(projetoB.getId(), 1, 10, PESQUISADOR));
    }

    // ---------------------------------------------------------------- membros do grupo

    @Test
    void membrosDoGrupoSoParaOLider() {
        logarComo(PESQUISADOR);
        assertThrows(SemPermissaoNoGrupoErro.class, () -> grupoService.listarPesquisadores(grupo.getId(), 1, 50, null));

        logarComo(LIDER);
        assertEquals(4, grupoService.listarPesquisadores(grupo.getId(), 1, 50, null).totalElements());
    }

    // ---------------------------------------------------------------- colaborador somente leitura

    @Test
    void colaboradorNaoMarcaFavorito() {
        logarComo(COLABORADOR);
        assertThrows(SemPermissaoNoGrupoErro.class, () -> projetoFachada.alternarFavorito(projetoA.getId()));
    }

    @Test
    void somenteLideresCriamSolicitacao() {
        CriarSolicitacaoRequest request = new CriarSolicitacaoRequest("Diárias", "Coleta", "Justificativa",
                "financiamento", projetoA.getId(), null, null, BigDecimal.TEN);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> solicitacaoService.criar(request, COLABORADOR));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> solicitacaoService.criar(request, PESQUISADOR));
        assertNotNull(solicitacaoService.criar(request, LIDER));
    }

    @Test
    void colaboradorNaoConfirmaPresenca() {
        LembreteResponse lembrete = lembreteService.criar(new CreateLembreteRequest(grupo.getId(), "Reunião geral",
                "Descrição", TipoLembrete.REUNIAO, LocalDateTime.now().plusDays(2), null, null, null, null, null,
                null, null, null, true), LIDER);

        assertThrows(SemPermissaoNoGrupoErro.class, () -> lembreteService.confirmarPresenca(lembrete.id(), COLABORADOR));
        assertNotNull(lembreteService.confirmarPresenca(lembrete.id(), PESQUISADOR));
    }
}
