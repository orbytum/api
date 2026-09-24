package com.orbytum.api.service;

import com.orbytum.api.models.dto.response.AtividadeResponse;
import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AtividadeStatus;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.exceptions.TransicaoAtividadeInvalidaErro;
import com.orbytum.api.repository.AtividadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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

        responsavel = new Usuario("Responsável", "resp@test.com", "1", "Dr");

        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));
        lenient().when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Atividade atividade(AtividadeStatus status, LocalDateTime prazo) {
        Atividade atividade = new Atividade(projeto, responsavel, null, "Título", "Descrição", prazo);
        atividade.setId(1L);
        atividade.setStatus(status);
        return atividade;
    }

    @Test
    void transicaoInvalidaLancaErro() {
        when(atividadeRepository.findById(1L)).thenReturn(Optional.of(atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1))));

        assertThrows(TransicaoAtividadeInvalidaErro.class,
                () -> atividadeService.atualizarStatus(1L, AtividadeStatus.CONCLUIDA, EMAIL));
    }

    @Test
    void transicaoValidaAtualizaStatus() {
        when(atividadeRepository.findById(1L)).thenReturn(Optional.of(atividade(AtividadeStatus.PENDENTE, LocalDateTime.now().plusDays(1))));

        AtividadeResponse response = atividadeService.atualizarStatus(1L, AtividadeStatus.EM_ANDAMENTO, EMAIL);

        assertEquals(AtividadeStatus.EM_ANDAMENTO, response.status());
    }

    @Test
    void concluirExigeCoordenador() {
        when(atividadeRepository.findById(1L)).thenReturn(Optional.of(atividade(AtividadeStatus.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().plusDays(1))));
        doThrow(new SemPermissaoNoGrupoErro("Sem permissão"))
                .when(grupoAcessoService).validarCoordenadorOuAcima(anyLong(), any());

        assertThrows(SemPermissaoNoGrupoErro.class,
                () -> atividadeService.atualizarStatus(1L, AtividadeStatus.CONCLUIDA, EMAIL));
    }

    @Test
    void atividadeComPrazoVencidoEhMarcadaComoAtrasada() {
        when(atividadeRepository.findById(1L))
                .thenReturn(Optional.of(atividade(AtividadeStatus.EM_ANDAMENTO, LocalDateTime.now().minusDays(1))));

        AtividadeResponse response = atividadeService.buscar(1L, EMAIL);

        assertTrue(response.isAtrasada());
    }
}
