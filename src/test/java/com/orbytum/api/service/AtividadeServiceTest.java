package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateAtividadeRequest;
import com.orbytum.api.models.dto.request.EditAtividadeRequest;
import com.orbytum.api.models.dto.response.AtividadePaginadoResponse;
import com.orbytum.api.models.dto.response.AtividadeResponse;
import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.AtividadeStatus;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.exceptions.ResponsavelNaoPertenceAoProjetoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.exceptions.TransicaoAtividadeInvalidaErro;
import com.orbytum.api.repository.AtividadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    private static final String EMAIL = "user@test.com";

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoAcessoService grupoAcessoService;

    @InjectMocks
    private AtividadeService atividadeService;

    private Projeto projeto;
    private Usuario responsavel;

    @BeforeEach
    void setUp() {
        Grupo grupo = new Grupo("Grupo", true);
        grupo.setId(1L);

        projeto = new Projeto(grupo, ProjetoStatus.EM_ANDAMENTO, "Projeto", "Assunto");
        projeto.setId(5L);

        CredenciaisLogin credenciais = new CredenciaisLogin(
                "resp@test.com", "hash", AccessLevel.USER, new Usuario(), null);
        responsavel = new Usuario(10L, "Responsável", "resp@test.com", "1", "Dr",
                LocalDateTime.now(), true, credenciais);

        lenient().when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));
        lenient().when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(atividadeRepository.countByProjetoIdAndStatusInAndIsAtivoTrue(anyLong(), any()))
                .thenReturn(1L);
    }

    private Atividade atividade(AtividadeStatus status, LocalDateTime prazo) {
        Atividade atividade = new Atividade(projeto, responsavel, null, "Título", "Descrição", prazo);
        atividade.setId(1L);
        atividade.setStatus(status);
        return atividade;
    }

    private CreateAtividadeRequest createRequest(Long responsavelId, Long atividadePaiId) {
        return new CreateAtividadeRequest(5L, responsavelId, atividadePaiId, "Título", "Descrição",
                LocalDateTime.now().plusDays(7));
    }

    @Test
    void transicaoInvalidaLancaErro() {
        when(atividadeRepository.findByIdComRelacionamentos(1L))
                .thenReturn(Optional.of(atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1))));

        assertThrows(TransicaoAtividadeInvalidaErro.class,
                () -> atividadeService.atualizarStatus(1L, AtividadeStatus.CONCLUIDA, EMAIL));
    }

    @Test
    void transicaoValidaAtualizaStatus() {
        when(atividadeRepository.findByIdComRelacionamentos(1L))
                .thenReturn(Optional.of(atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1))));

        AtividadeResponse response = atividadeService.atualizarStatus(1L, AtividadeStatus.EM_ANDAMENTO, EMAIL);

        assertEquals(AtividadeStatus.EM_ANDAMENTO, response.status());
    }

    @Test
    void concluirExigeCoordenador() {
        when(atividadeRepository.findByIdComRelacionamentos(1L))
                .thenReturn(Optional.of(atividade(AtividadeStatus.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().plusDays(1))));
        doThrow(new SemPermissaoNoGrupoErro("Sem permissão"))
                .when(grupoAcessoService).validarCoordenadorOuAcima(anyLong(), any());

        assertThrows(SemPermissaoNoGrupoErro.class,
                () -> atividadeService.atualizarStatus(1L, AtividadeStatus.CONCLUIDA, EMAIL));
    }

    @Test
    void atividadeComPrazoVencidoEhMarcadaComoAtrasada() {
        when(atividadeRepository.findByIdComRelacionamentos(1L))
                .thenReturn(Optional.of(atividade(AtividadeStatus.EM_ANDAMENTO, LocalDateTime.now().minusDays(1))));

        AtividadeResponse response = atividadeService.buscar(1L, EMAIL);

        assertTrue(response.isAtrasada());
    }

    @Test
    void atividadeConcluidaComPrazoVencidoNaoEhAtrasada() {
        Atividade concluida = atividade(AtividadeStatus.ENCERRADA, LocalDateTime.now().minusDays(10));
        when(atividadeRepository.findByIdComRelacionamentos(1L)).thenReturn(Optional.of(concluida));

        AtividadeResponse response = atividadeService.buscar(1L, EMAIL);

        assertFalse(response.isAtrasada());
    }

    @Test
    void criarAtividadeDeTopoComResponsavelForaDoProjetoLancaErro() {
        when(projetoService.findById(5L)).thenReturn(Optional.of(projeto));
        when(usuarioService.findById(10L)).thenReturn(Optional.of(responsavel));
        when(grupoAcessoService.isMembroDoProjeto(5L, 10L)).thenReturn(false);

        assertThrows(ResponsavelNaoPertenceAoProjetoErro.class,
                () -> atividadeService.criar(createRequest(10L, null), EMAIL));
    }

    @Test
    void criarAtividadeDeTopoComResponsavelNoProjeto() {
        when(projetoService.findById(5L)).thenReturn(Optional.of(projeto));
        when(usuarioService.findById(10L)).thenReturn(Optional.of(responsavel));
        when(grupoAcessoService.isMembroDoProjeto(5L, 10L)).thenReturn(true);

        AtividadeResponse response = atividadeService.criar(createRequest(10L, null), EMAIL);

        assertEquals(AtividadeStatus.PENDENTE, response.status());
        assertEquals(5L, response.projetoId());
        assertEquals(10L, response.responsavelId());
        assertNotNull(response.dthRegistro());
    }

    @Test
    void atualizarResponsavelForaDoProjetoLancaErro() {
        when(atividadeRepository.findByIdComRelacionamentos(1L))
                .thenReturn(Optional.of(atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1))));
        when(usuarioService.findById(99L)).thenReturn(Optional.of(responsavel));
        when(grupoAcessoService.isMembroDoProjeto(5L, 10L)).thenReturn(false);

        EditAtividadeRequest request = new EditAtividadeRequest(99L, "Novo título", "Nova descrição", null);

        assertThrows(ResponsavelNaoPertenceAoProjetoErro.class,
                () -> atividadeService.atualizar(1L, request, EMAIL));
    }

    @Test
    void criarImpedimentoAceitaQualquerMembroDoGrupo() {
        Atividade pai = atividade(AtividadeStatus.EM_ANDAMENTO, LocalDateTime.now().plusDays(1));
        pai.setId(7L);

        when(projetoService.findById(5L)).thenReturn(Optional.of(projeto));
        when(usuarioService.findById(10L)).thenReturn(Optional.of(responsavel));
        when(atividadeRepository.findByIdComRelacionamentos(7L)).thenReturn(Optional.of(pai));

        AtividadeResponse response = atividadeService.criar(createRequest(10L, 7L), EMAIL);

        assertEquals(7L, response.atividadePaiId());
        verify(grupoAcessoService).validarMembro(1L, 10L);
        verify(grupoAcessoService, never()).isMembroDoProjeto(anyLong(), anyLong());
    }

    @Test
    void criarImpedimentoEmAtividadeQueNaoEstaEmAndamentoLancaErro() {
        Atividade pai = atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1));
        pai.setId(7L);

        when(projetoService.findById(5L)).thenReturn(Optional.of(projeto));
        when(usuarioService.findById(10L)).thenReturn(Optional.of(responsavel));
        when(atividadeRepository.findByIdComRelacionamentos(7L)).thenReturn(Optional.of(pai));

        assertThrows(TransicaoAtividadeInvalidaErro.class,
                () -> atividadeService.criar(createRequest(10L, 7L), EMAIL));
    }

    @Test
    void deletarFazSoftDeleteSemRemoverLinha() {
        Atividade alvo = atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1));
        when(atividadeRepository.findByIdComRelacionamentos(1L)).thenReturn(Optional.of(alvo));

        atividadeService.deletar(1L, EMAIL);

        assertFalse(alvo.isAtivo());
        verify(atividadeRepository, never()).delete(any(Atividade.class));
    }

    @Test
    void listarPorProjetoRetornaPaginaComFlagCalculadaUmaVez() {
        List<Atividade> conteudo = List.of(
                atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1)),
                atividade(AtividadeStatus.EM_ANDAMENTO, LocalDateTime.now().plusDays(2)));
        when(projetoService.findById(5L)).thenReturn(Optional.of(projeto));
        when(atividadeRepository.pageAtivasPorProjeto(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(conteudo));

        AtividadePaginadoResponse response = atividadeService.listarPorProjeto(5L, 1, 10, EMAIL);

        assertEquals(2, response.items().size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.currentPage());
        assertEquals(10, response.pageSize());
        assertTrue(response.items().stream().noneMatch(AtividadeResponse::projetoPodeSerFinalizado));
        verify(atividadeRepository, times(1))
                .countByProjetoIdAndStatusInAndIsAtivoTrue(anyLong(), any());
    }

    @Test
    void listarAtrasadasFiltraNoBancoComStatusAbertos() {
        when(projetoService.findById(5L)).thenReturn(Optional.of(projeto));
        when(atividadeRepository.findAllAtrasadasPorProjeto(eq(5L), any(), any(LocalDateTime.class)))
                .thenReturn(List.of(atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().minusDays(3))));

        List<AtividadeResponse> response = atividadeService.listarAtrasadasPorProjeto(5L, EMAIL);

        assertEquals(1, response.size());
        assertTrue(response.get(0).isAtrasada());
        verify(atividadeRepository).findAllAtrasadasPorProjeto(eq(5L), eq(List.of(
                AtividadeStatus.PENDENTE,
                AtividadeStatus.EM_ANDAMENTO,
                AtividadeStatus.AGUARDANDO_CONFIRMACAO)), any(LocalDateTime.class));
    }
}
