package com.orbytum.api;

import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.response.LembretePaginadoResponse;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.repository.EventoParticipanteRepository;
import com.orbytum.api.repository.EventoRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.LembreteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.LembreteNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class LembreteEscopoIntegrationTest {

    private static final String LIDER_EMAIL = "lembrete.lider@teste.com";
    private static final String LIDER_PROJETO_EMAIL = "lembrete.projeto@teste.com";
    private static final String MEMBRO_EMAIL = "lembrete.membro@teste.com";

    @Autowired
    private LembreteService lembreteService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private EventoParticipanteRepository eventoParticipanteRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ProjetoXUsuarioRepository projetoXUsuarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Grupo grupo;
    private Projeto projeto;
    private Role role;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            role = roleRepository.save(new Role("Membro Lembrete", List.of(), false));

            Usuario lider = usuarioRepository.save(new Usuario("Líder Lembrete", LIDER_EMAIL, "11955550001", "Dr"));
            Usuario liderProjeto = usuarioRepository.save(new Usuario("Líder Projeto", LIDER_PROJETO_EMAIL, "11955550002", "Ms"));
            Usuario membro = usuarioRepository.save(new Usuario("Membro", MEMBRO_EMAIL, "11955550003", "Ms"));

            grupo = grupoRepository.save(new Grupo("Grupo Lembretes Escopo", lider));
            projeto = projetoRepository.save(new Projeto(
                    grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto Lembrete", "Assunto"));

            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, lider, role, NivelMembro.LIDER, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, liderProjeto, role, NivelMembro.PESQUISADOR, null, true));
            grupoXUsuarioRepository.save(new GrupoXUsuario(grupo, membro, role, NivelMembro.PESQUISADOR, null, true));

            projetoXUsuarioRepository.save(new ProjetoXUsuario(projeto, liderProjeto, NivelMembro.LIDER));
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            eventoParticipanteRepository.deleteAll();
            eventoRepository.deleteAll();
            projetoXUsuarioRepository.deleteAll();
            projetoRepository.deleteAll();
            grupoXUsuarioRepository.deleteAll();
            grupoRepository.deleteAll();
            usuarioRepository.deleteAll();
            if (role != null) {
                roleRepository.deleteById(role.getId());
            }
        });
    }

    @Test
    void membroQueNaoParticipaDoProjetoVeApenasLembreteDoGrupoInteiro() {
        LembreteResponse grupoInteiro = lembreteService.criar(
                request("Reunião geral", null, true), LIDER_EMAIL);
        LembreteResponse doProjeto = lembreteService.criar(
                request("Reunião do projeto", projeto.getId(), false), LIDER_PROJETO_EMAIL);

        assertEquals(null, grupoInteiro.projetoId());
        assertFalse(doProjeto.paraGrupoInteiro());

        LembretePaginadoResponse listagem = lembreteService.listarPorGrupo(
                grupo.getId(), null, null, null, null, null, 1, 10, MEMBRO_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals(grupoInteiro.id(), listagem.items().get(0).id());
    }

    @Test
    void filtroPorProjetoRetornaSomenteLembretesDoProjeto() {
        lembreteService.criar(request("Reunião geral", null, true), LIDER_EMAIL);
        LembreteResponse doProjeto = lembreteService.criar(
                request("Reunião do projeto", projeto.getId(), false), LIDER_PROJETO_EMAIL);

        LembretePaginadoResponse listagem = lembreteService.listarPorGrupo(
                grupo.getId(), projeto.getId(), null, null, null, null, 1, 10, LIDER_PROJETO_EMAIL);

        assertEquals(1, listagem.totalElements());
        assertEquals(doProjeto.id(), listagem.items().get(0).id());
        assertEquals(projeto.getId(), listagem.items().get(0).projetoId());
    }

    private CreateLembreteRequest request(String titulo, Long projetoId, boolean paraGrupoInteiro) {
        return new CreateLembreteRequest(
                grupo.getId(),
                titulo,
                "Descrição",
                TipoLembrete.REUNIAO,
                LocalDateTime.now().plusDays(3),
                null, null, null, null, null, null, null,
                projetoId,
                paraGrupoInteiro
        );
    }

    // ---------------------------------------------------------------- REQ-093: detalhe do lembrete

    @Test
    void membroQueNaoParticipaNaoAbreLembreteDoProjeto() {
        LembreteResponse doProjeto = lembreteService.criar(
                request("Encontro do projeto", projeto.getId(), false), LIDER_EMAIL);

        assertThrows(LembreteNaoEncontradoErro.class, () -> lembreteService.buscar(doProjeto.id(), MEMBRO_EMAIL));
        assertEquals(doProjeto.id(), lembreteService.buscar(doProjeto.id(), LIDER_PROJETO_EMAIL).id());
        assertEquals(doProjeto.id(), lembreteService.buscar(doProjeto.id(), LIDER_EMAIL).id());
    }

    // ---------------------------------------------------------------- REQ-069: assinatura da agenda

    private String tokenDaAssinatura(String email) {
        String url = lembreteService.gerarAssinatura(grupo.getId(), email).url();
        assertTrue(url.contains("/calendario/feed/"), url);
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Test
    void feedDaAssinaturaRespeitaOEscopoDoUsuario() {
        lembreteService.criar(request("Encontro aberto", null, true), LIDER_EMAIL);
        lembreteService.criar(request("Encontro do projeto", projeto.getId(), false), LIDER_EMAIL);

        String icsMembro = new String(lembreteService.exportarIcsAssinatura(tokenDaAssinatura(MEMBRO_EMAIL)));
        String icsLiderProjeto = new String(lembreteService.exportarIcsAssinatura(tokenDaAssinatura(LIDER_PROJETO_EMAIL)));

        assertTrue(icsMembro.contains("Encontro aberto"));
        assertFalse(icsMembro.contains("Encontro do projeto"));
        assertTrue(icsLiderProjeto.contains("Encontro aberto"));
        assertTrue(icsLiderProjeto.contains("Encontro do projeto"));
    }

    @Test
    void feedComTokenInvalidoEhRejeitado() {
        assertThrows(GrupoNaoEncontradoErro.class, () -> lembreteService.exportarIcsAssinatura("abc.def.ghi"));
    }

    @Test
    void feedDeixaDeFuncionarQuandoUsuarioSaiDoGrupo() {
        String token = tokenDaAssinatura(MEMBRO_EMAIL);
        transactionTemplate.executeWithoutResult(status -> {
            Usuario membro = usuarioRepository.findByEmail(MEMBRO_EMAIL).orElseThrow();
            GrupoXUsuario vinculo = grupoXUsuarioRepository
                    .findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupo.getId(), membro.getId()).orElseThrow();
            vinculo.setAtivo(false);
            grupoXUsuarioRepository.save(vinculo);
        });

        assertThrows(SemPermissaoNoGrupoErro.class, () -> lembreteService.exportarIcsAssinatura(token));
    }
}
