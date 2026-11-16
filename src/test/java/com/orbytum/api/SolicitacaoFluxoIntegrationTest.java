package com.orbytum.api;

import com.orbytum.api.models.dto.request.CriarSolicitacaoRequest;
import com.orbytum.api.models.dto.response.SolicitacaoResponse;
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
import com.orbytum.api.models.enums.SolicitacaoStatus;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.SolicitacaoRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.SolicitacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fluxo da solicitação: o líder do projeto solicita, o Líder do grupo aprova a primeira etapa e o
 * administrador decide; quando o solicitante já é o Líder do grupo, a primeira etapa é dispensada.
 */
@SpringBootTest
class SolicitacaoFluxoIntegrationTest {

    private static final String ADMIN = "fluxo.admin@teste.com";
    private static final String LIDER_GRUPO = "fluxo.lidergrupo@teste.com";
    private static final String LIDER_PROJETO = "fluxo.liderprojeto@teste.com";
    private static final String PESQUISADOR = "fluxo.pesquisador@teste.com";

    @Autowired private SolicitacaoService solicitacaoService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CredenciaisLoginRepository credenciaisLoginRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private GrupoRepository grupoRepository;
    @Autowired private GrupoXUsuarioRepository grupoXUsuarioRepository;
    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private ProjetoXUsuarioRepository projetoXUsuarioRepository;
    @Autowired private SolicitacaoRepository solicitacaoRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Grupo grupo;
    private Projeto projeto;
    private Role role;

    private Usuario usuario(String nome, String email, String telefone, AccessLevel nivel) {
        Usuario u = usuarioRepository.save(new Usuario(nome, email, telefone, "Ms"));
        credenciaisLoginRepository.save(new CredenciaisLogin(email, "senha", nivel, u, null));
        return u;
    }

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            role = roleRepository.save(new Role("Membro Fluxo", List.of(), false));
            usuario("Admin", ADMIN, "11955550001", AccessLevel.ADMIN);
            Usuario liderGrupo = usuario("Líder do grupo", LIDER_GRUPO, "11955550002", AccessLevel.USER);
            Usuario liderProjeto = usuario("Líder do projeto", LIDER_PROJETO, "11955550003", AccessLevel.USER);
            Usuario pesquisador = usuario("Pesquisador", PESQUISADOR, "11955550004", AccessLevel.USER);

            grupo = grupoRepository.save(new Grupo("Grupo Fluxo Solicitação", liderGrupo));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, liderGrupo, role, NivelMembro.LIDER, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, liderProjeto, role, NivelMembro.PESQUISADOR, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, pesquisador, role, NivelMembro.PESQUISADOR, null, true));

            projeto = projetoRepository.save(new Projeto(grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto Fluxo", "Assunto"));
            projetoXUsuarioRepository.save(new ProjetoXUsuario(projeto, liderProjeto, NivelMembro.LIDER));
            projetoXUsuarioRepository.save(new ProjetoXUsuario(projeto, pesquisador, NivelMembro.PESQUISADOR));
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            solicitacaoRepository.deleteAll();
            projetoXUsuarioRepository.deleteAll();
            projetoRepository.deleteAll();
            grupoXUsuarioRepository.deleteAll();
            grupoRepository.deleteAll();
            credenciaisLoginRepository.deleteAll();
            usuarioRepository.deleteAll();
            roleRepository.deleteById(role.getId());
        });
    }

    private CriarSolicitacaoRequest pedido() {
        return new CriarSolicitacaoRequest("Diárias de campo", "Coleta em Ponta Grossa", "Coleta de amostras",
                "financiamento", projeto.getId(), null, null, new BigDecimal("1200.00"));
    }

    @Test
    void liderDoProjetoSolicitaLiderDoGrupoAprovaEAdministradorDecide() {
        SolicitacaoResponse criada = solicitacaoService.criar(pedido(), LIDER_PROJETO);
        assertEquals(SolicitacaoStatus.PENDENTE, criada.status());
        assertEquals("Diárias de campo", criada.titulo());

        // O administrador não pula a primeira etapa.
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.aprovar(criada.id(), ADMIN));

        SolicitacaoResponse aprovadaPeloLider = solicitacaoService.aprovar(criada.id(), LIDER_GRUPO);
        assertEquals(SolicitacaoStatus.AGUARDANDO_ADMINISTRADOR, aprovadaPeloLider.status());
        assertEquals("Líder do grupo", aprovadaPeloLider.aprovadorLiderNome());
        assertFalse(aprovadaPeloLider.isAprovada());

        // Na segunda etapa, o Líder do grupo não decide de novo.
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.aprovar(criada.id(), LIDER_GRUPO));

        SolicitacaoResponse decidida = solicitacaoService.aprovar(criada.id(), ADMIN);
        assertEquals(SolicitacaoStatus.CONCLUIDA, decidida.status());
        assertTrue(decidida.isAprovada());
    }

    @Test
    void solicitacaoDoLiderDoGrupoVaiDiretoAoAdministrador() {
        SolicitacaoResponse criada = solicitacaoService.criar(pedido(), LIDER_GRUPO);

        assertEquals(SolicitacaoStatus.AGUARDANDO_ADMINISTRADOR, criada.status());
        assertNotNull(criada.dthAprovacaoLider());
        assertEquals(SolicitacaoStatus.REJEITADA, solicitacaoService.rejeitar(criada.id(), ADMIN).status());
    }

    @Test
    void pesquisadorNaoSolicitaNemAprova() {
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.criar(pedido(), PESQUISADOR));

        SolicitacaoResponse criada = solicitacaoService.criar(pedido(), LIDER_PROJETO);
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.aprovar(criada.id(), PESQUISADOR));
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.aprovar(criada.id(), LIDER_PROJETO));
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.rejeitar(criada.id(), LIDER_PROJETO));
    }

    @Test
    void liderDoGrupoRejeitaNaPrimeiraEtapa() {
        SolicitacaoResponse criada = solicitacaoService.criar(pedido(), LIDER_PROJETO);

        assertEquals(SolicitacaoStatus.REJEITADA, solicitacaoService.rejeitar(criada.id(), LIDER_GRUPO).status());
        assertThrows(IllegalArgumentException.class, () -> solicitacaoService.aprovar(criada.id(), ADMIN));
    }

    @Test
    void autorNaoAlteraDepoisDaAprovacaoDoLider() {
        SolicitacaoResponse criada = solicitacaoService.criar(pedido(), LIDER_PROJETO);
        solicitacaoService.aprovar(criada.id(), LIDER_GRUPO);

        assertThrows(AccessDeniedException.class, () -> solicitacaoService.remover(criada.id(), LIDER_PROJETO));
    }

    @Test
    void listagemDoGrupoEDoAdministrador() {
        solicitacaoService.criar(pedido(), LIDER_PROJETO);
        solicitacaoService.criar(pedido(), LIDER_GRUPO);

        assertEquals(2, solicitacaoService.listarPorGrupo(grupo.getId(), LIDER_GRUPO).size());
        assertEquals(2, solicitacaoService.listarPorGrupo(grupo.getId(), PESQUISADOR).size());
        assertEquals(2, solicitacaoService.listarTodas(ADMIN).size());
        assertThrows(AccessDeniedException.class, () -> solicitacaoService.listarTodas(LIDER_GRUPO));
    }
}
