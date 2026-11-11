package com.orbytum.api;

import com.orbytum.api.models.dto.request.CreateAtividadeRequest;
import com.orbytum.api.models.dto.request.EditAtividadeRequest;
import com.orbytum.api.models.dto.response.AtividadePaginadoResponse;
import com.orbytum.api.models.dto.response.AtividadeResponse;
import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.AtividadeStatus;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.exceptions.ResponsavelNaoPertenceAoProjetoErro;
import com.orbytum.api.models.exceptions.TransicaoAtividadeInvalidaErro;
import com.orbytum.api.repository.AtividadeRepository;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.AtividadeService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sem {@code @Transactional} de classe: a fixture e commitada em {@code @BeforeEach} e os
 * testes chamam o service sem transacao ambiente. Assim o unico escopo de persistencia vem das
 * anotacoes de {@code AtividadeService}, que e exatamente o que valida o carregamento de
 * {@code responsavel} (LAZY) nos caminhos de leitura.
 */
@SpringBootTest
public class AtividadeIntegrationTest {

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ProjetoXUsuarioRepository projetoXUsuarioRepository;

    @Autowired
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CredenciaisLoginRepository credenciaisLoginRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String LIDER_EMAIL = "ativ.lider@teste.com";
    private static final String PESQUISADOR_EMAIL = "ativ.pesquisador@teste.com";
    private static final String ESTRANGEIRO_EMAIL = "ativ.estrangeiro@teste.com";

    private Grupo grupo;
    private Projeto projeto;
    private Usuario lider;
    private Usuario pesquisador;
    private Usuario estrangeiro;
    private Role role;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            role = roleRepository.save(new Role("Pesquisador", List.of(), false));

            lider = usuarioRepository.save(new Usuario("Líder Ativ", LIDER_EMAIL, "11977770001", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    lider.getEmail(), "senha123", AccessLevel.USER, lider, null));

            pesquisador = usuarioRepository.save(new Usuario("Pesquisador Ativ", PESQUISADOR_EMAIL, "11977770002", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    pesquisador.getEmail(), "senha123", AccessLevel.USER, pesquisador, null));

            estrangeiro = usuarioRepository.save(new Usuario("Estrangeiro Ativ", ESTRANGEIRO_EMAIL, "11977770003", "Dr"));
            credenciaisLoginRepository.save(new CredenciaisLogin(
                    estrangeiro.getEmail(), "senha123", AccessLevel.USER, estrangeiro, null));

            grupo = grupoRepository.save(new Grupo("Grupo Atividades", lider));
            projeto = projetoRepository.save(new Projeto(grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto", "Assunto"));

            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, lider, role, NivelMembro.COORDENADOR, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, pesquisador, role, NivelMembro.PESQUISADOR, null, true));

            projetoXUsuarioRepository.save(new ProjetoXUsuario(projeto, lider, NivelMembro.COORDENADOR));
            projetoXUsuarioRepository.save(new ProjetoXUsuario(projeto, pesquisador, NivelMembro.PESQUISADOR));
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            projetoXUsuarioRepository.deleteAll();
            atividadeRepository.deleteAll();
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

    private AtividadeResponse criarAtividade(Long responsavelId, LocalDateTime prazo) {
        return atividadeService.criar(
                new CreateAtividadeRequest(projeto.getId(), responsavelId, null, "Coletar amostras",
                        "Coletar 10 amostras", prazo),
                LIDER_EMAIL);
    }

    @Test
    void criarEBuscarCarregaResponsavelSemLazyInitializationException() {
        AtividadeResponse criada = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));

        AtividadeResponse buscada = atividadeService.buscar(criada.id(), LIDER_EMAIL);

        assertEquals(criada.id(), buscada.id());
        assertEquals(lider.getId(), buscada.responsavelId());
        assertEquals("Líder Ativ", buscada.responsavelNome());
        assertNotNull(buscada.dthRegistro());
        assertNull(buscada.dthConclusao());
        assertFalse(buscada.isAtrasada());
    }

    @Test
    void listarPorProjetoCarregaResponsavelDeTodosOsItens() {
        criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));
        criarAtividade(pesquisador.getId(), LocalDateTime.now().plusDays(6));

        AtividadePaginadoResponse pagina = atividadeService.listarPorProjeto(projeto.getId(), 1, 10, LIDER_EMAIL);

        assertEquals(2, pagina.items().size());
        assertEquals(2, pagina.totalElements());
        assertEquals(1, pagina.totalPages());
        assertTrue(pagina.items().stream().allMatch(i -> i.responsavelNome() != null));
    }

    @Test
    void criarComResponsavelForaDoProjetoLancaErro() {
        assertThrows(ResponsavelNaoPertenceAoProjetoErro.class,
                () -> criarAtividade(estrangeiro.getId(), LocalDateTime.now().plusDays(5)));

        assertEquals(0, atividadeRepository.count());
    }

    @Test
    void fluxoCompletoDeStatusGravaDataDeConclusaoESinalizaFimDoProjeto() {
        AtividadeResponse primeira = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));
        AtividadeResponse segunda = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(6));

        atividadeService.atualizarStatus(primeira.id(), AtividadeStatus.EM_ANDAMENTO, LIDER_EMAIL);
        atividadeService.atualizarStatus(primeira.id(), AtividadeStatus.AGUARDANDO_CONFIRMACAO, LIDER_EMAIL);
        AtividadeResponse concluida = atividadeService.atualizarStatus(
                primeira.id(), AtividadeStatus.CONCLUIDA, LIDER_EMAIL);

        assertEquals(AtividadeStatus.CONCLUIDA, concluida.status());
        assertNotNull(concluida.dthConclusao());
        assertFalse(concluida.isAtrasada());
        assertFalse(concluida.projetoPodeSerFinalizado());

        atividadeService.atualizarStatus(segunda.id(), AtividadeStatus.EM_ANDAMENTO, LIDER_EMAIL);
        atividadeService.atualizarStatus(segunda.id(), AtividadeStatus.AGUARDANDO_CONFIRMACAO, LIDER_EMAIL);
        AtividadeResponse ultimaAberta = atividadeService.atualizarStatus(
                segunda.id(), AtividadeStatus.CONCLUIDA, LIDER_EMAIL);

        assertTrue(ultimaAberta.projetoPodeSerFinalizado());

        AtividadeResponse encerrada = atividadeService.atualizarStatus(
                segunda.id(), AtividadeStatus.ENCERRADA, LIDER_EMAIL);

        assertEquals(AtividadeStatus.ENCERRADA, encerrada.status());
        assertTrue(encerrada.projetoPodeSerFinalizado());
    }

    @Test
    void atividadeAtrasadaComStatusAbertoEhDestacada() {
        LocalDateTime prazoVencido = LocalDateTime.now().minusDays(2);

        criarAtividade(lider.getId(), prazoVencido);

        List<AtividadeResponse> atrasadas = atividadeService.listarAtrasadasPorProjeto(projeto.getId(), LIDER_EMAIL);
        assertEquals(1, atrasadas.size());
        assertTrue(atrasadas.get(0).isAtrasada());
    }

    @Test
    void atividadeAtrasadaConcluidaNaoApareceNaListaDeAtrasadas() {
        AtividadeResponse criada = criarAtividade(lider.getId(), LocalDateTime.now().minusDays(2));
        atividadeService.atualizarStatus(criada.id(), AtividadeStatus.EM_ANDAMENTO, LIDER_EMAIL);
        atividadeService.atualizarStatus(criada.id(), AtividadeStatus.AGUARDANDO_CONFIRMACAO, LIDER_EMAIL);
        atividadeService.atualizarStatus(criada.id(), AtividadeStatus.CONCLUIDA, LIDER_EMAIL);

        assertTrue(atividadeService.listarAtrasadasPorProjeto(projeto.getId(), LIDER_EMAIL).isEmpty());
    }

    @Test
    void transicaoInvalidaEhRejeitada() {
        AtividadeResponse criada = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));

        assertThrows(TransicaoAtividadeInvalidaErro.class,
                () -> atividadeService.atualizarStatus(criada.id(), AtividadeStatus.ENCERRADA, LIDER_EMAIL));
    }

    @Test
    void atualizarResponsavelForaDoProjetoLancaErro() {
        AtividadeResponse criada = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));

        assertThrows(ResponsavelNaoPertenceAoProjetoErro.class,
                () -> atividadeService.atualizar(
                        criada.id(),
                        new EditAtividadeRequest(estrangeiro.getId(), "Novo", "Novo texto", null),
                        LIDER_EMAIL));
    }

    @Test
    void softDeleteRemoveDasListagens() {
        AtividadeResponse criada = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));

        atividadeService.deletar(criada.id(), LIDER_EMAIL);

        Atividade entidade = atividadeRepository.findById(criada.id()).orElseThrow();
        assertFalse(entidade.isAtivo());
        assertEquals(0, atividadeService.listarPorProjeto(projeto.getId(), 1, 10, LIDER_EMAIL).totalElements());
    }

    @Test
    void paginaRespeitaPageESize() {
        for (int i = 1; i <= 5; i++) {
            criarAtividade(lider.getId(), LocalDateTime.now().plusDays(i));
        }

        AtividadePaginadoResponse primeira = atividadeService.listarPorProjeto(projeto.getId(), 1, 2, LIDER_EMAIL);
        AtividadePaginadoResponse terceira = atividadeService.listarPorProjeto(projeto.getId(), 3, 2, LIDER_EMAIL);

        assertEquals(2, primeira.items().size());
        assertEquals(5, primeira.totalElements());
        assertEquals(3, primeira.totalPages());
        assertEquals(1, primeira.currentPage());
        assertEquals(2, primeira.pageSize());
        assertEquals(1, terceira.items().size());
        assertEquals(3, terceira.currentPage());
    }

    @Test
    void membroDeOutroGrupoNaoAcessaAtividade() {
        AtividadeResponse criada = criarAtividade(lider.getId(), LocalDateTime.now().plusDays(5));

        assertThrows(RuntimeException.class, () -> atividadeService.buscar(criada.id(), ESTRANGEIRO_EMAIL));
    }
}
