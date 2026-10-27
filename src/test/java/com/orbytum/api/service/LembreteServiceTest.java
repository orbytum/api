package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.request.EditLembreteRequest;
import com.orbytum.api.models.dto.response.LembretePaginadoResponse;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Evento;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.StatusLembrete;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.EventoParticipanteRepository;
import com.orbytum.api.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LembreteServiceTest {

    private static final String EMAIL_CRIADOR = "criador@orbytum.com";
    private static final String EMAIL_OUTRO = "outro@orbytum.com";

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private EventoParticipanteRepository participanteRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoAcessoService grupoAcessoService;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private LembreteService lembreteService;

    private Usuario criador;
    private Usuario outroUsuario;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        CredenciaisLogin credCriador = new CredenciaisLogin(
                EMAIL_CRIADOR, "hash", AccessLevel.USER, null, null);
        criador = new Usuario(1L, "Carlos Pesquisador", EMAIL_CRIADOR, "1199999999", "Dr",
                LocalDateTime.now(), true, credCriador);

        CredenciaisLogin credOutro = new CredenciaisLogin(
                EMAIL_OUTRO, "hash", AccessLevel.USER, null, null);
        outroUsuario = new Usuario(2L, "Outro Pesquisador", EMAIL_OUTRO, "1188888888", "Ms",
                LocalDateTime.now(), true, credOutro);

        grupo = new Grupo("Grupo de Inteligência Artificial", true);
        grupo.setId(10L);
    }

    private Evento criarEventoExemplo(Long id, TipoLembrete tipo, LocalDateTime dataHora) {
        return Evento.builder()
                .id(id)
                .grupo(grupo)
                .criador(criador)
                .titulo("Lembrete de Teste")
                .descricao("Descrição de teste")
                .tipo(tipo)
                .dthInicio(dataHora)
                .localizacao("Sala 101")
                .url("https://meet.google.com/xyz")
                .recorrencia(TipoRecorrencia.NENHUMA)
                .organizador("Carlos Pesquisador")
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .participantes(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("CA 1, CA 2, CA 3, CA 4, CA 5: Deve criar lembrete/evento com sucesso quando usuário for membro")
    void testCriarLembreteComSucesso() {
        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));

        LocalDateTime dataHora = LocalDateTime.now().plusDays(5);
        CreateLembreteRequest request = new CreateLembreteRequest(
                10L,
                "Reunião de Acompanhamento",
                "Discussão semanal do projeto",
                TipoLembrete.REUNIAO,
                dataHora,
                "Sala 302",
                null,
                "https://meet.google.com/abc",
                TipoRecorrencia.SEMANAL,
                "Carlos Pesquisador",
                List.of("Maria Silva", "João Santos"),
                null
        );

        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> {
            Evento e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        LembreteResponse response = lembreteService.criar(request, EMAIL_CRIADOR);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Reunião de Acompanhamento", response.titulo());
        assertEquals(TipoLembrete.REUNIAO, response.tipo());
        assertEquals(TipoRecorrencia.SEMANAL, response.recorrencia());
        assertEquals(StatusLembrete.AGENDADO, response.status());
        assertEquals(10L, response.grupoId());
        assertEquals(1L, response.criadorId());
        assertEquals(2, response.participantes().size());
        assertFalse(response.temAta());

        verify(grupoAcessoService).validarMembro(10L, 1L);
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    @DisplayName("CA 4: Deve lançar erro ao criar lembrete para grupo inexistente")
    void testCriarLembreteGrupoInexistenteLancaErro() {
        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(grupoRepository.findById(999L)).thenReturn(Optional.empty());

        CreateLembreteRequest request = new CreateLembreteRequest(
                999L,
                "Título",
                "Descrição",
                TipoLembrete.EDITAL,
                LocalDateTime.now().plusDays(2),
                null, null, null, null, null, null, null
        );

        assertThrows(GrupoNaoEncontradoErro.class, () -> lembreteService.criar(request, EMAIL_CRIADOR));
    }

    @Test
    @DisplayName("CA 1, CA 4: Deve lançar erro quando usuário não for membro do grupo")
    void testCriarLembreteUsuarioNaoMembroLancaErro() {
        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        doThrow(new SemPermissaoNoGrupoErro("Não é membro")).when(grupoAcessoService).validarMembro(10L, 1L);

        CreateLembreteRequest request = new CreateLembreteRequest(
                10L,
                "Título",
                "Descrição",
                TipoLembrete.WORKSHOP,
                LocalDateTime.now().plusDays(2),
                null, null, null, null, null, null, null
        );

        assertThrows(SemPermissaoNoGrupoErro.class, () -> lembreteService.criar(request, EMAIL_CRIADOR));
    }

    @Test
    @DisplayName("CA 6: Somente o criador pode atualizar o lembrete")
    void testAtualizarLembretePorCriadorComSucesso() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.APRESENTACAO, LocalDateTime.now().plusDays(2));

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        EditLembreteRequest request = new EditLembreteRequest(
                "Título Atualizado",
                "Nova Descrição",
                TipoLembrete.APRESENTACAO,
                LocalDateTime.now().plusDays(4),
                "Auditório B",
                null,
                null,
                TipoRecorrencia.MENSAL,
                "Novo Organizador",
                null,
                null
        );

        LembreteResponse response = lembreteService.atualizar(100L, request, EMAIL_CRIADOR);

        assertEquals("Título Atualizado", response.titulo());
        assertEquals("Auditório B", response.local());
        assertEquals(TipoRecorrencia.MENSAL, response.recorrencia());
    }

    @Test
    @DisplayName("CA 6: Tentativa de atualização por outro usuário deve ser rejeitada com 403 (SemPermissaoNoGrupoErro)")
    void testAtualizarLembretePorOutroUsuarioLancaSemPermissao() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.REUNIAO, LocalDateTime.now().plusDays(2));

        when(usuarioService.findByEmail(EMAIL_OUTRO)).thenReturn(Optional.of(outroUsuario));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));

        EditLembreteRequest request = new EditLembreteRequest(
                "Novo Título",
                null,
                TipoLembrete.REUNIAO,
                LocalDateTime.now().plusDays(2),
                null, null, null, null, null, null, null
        );

        assertThrows(SemPermissaoNoGrupoErro.class, () -> lembreteService.atualizar(100L, request, EMAIL_OUTRO));
    }

    @Test
    @DisplayName("CA 6: Não permite alterar o tipo para diferente de Reunião se houver ata anexada")
    void testAtualizarLembreteMudarTipoDeReuniaoComAtaLancaErro() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(1));
        existente.setAtaUrl("https://s3.aws.com/atas/ata.pdf");
        existente.setAtaS3Key("atas/100/ata.pdf");

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));

        EditLembreteRequest request = new EditLembreteRequest(
                "Tentando mudar para Edital",
                null,
                TipoLembrete.EDITAL,
                LocalDateTime.now().plusDays(2),
                null, null, null, null, null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> lembreteService.atualizar(100L, request, EMAIL_CRIADOR));
    }

    @Test
    @DisplayName("CA 6: Deve inserir arquivo de ata com sucesso em lembrete tipo Reunião quando solicitado pelo criador")
    void testInserirAtaEmReuniaoPorCriadorComSucesso() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(1));

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));
        when(s3StorageService.uploadFile(anyString(), any(MockMultipartFile.class)))
                .thenReturn("https://bucket.s3.amazonaws.com/atas/100/ata.pdf");
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "ata_reuniao.pdf",
                "application/pdf",
                "Conteúdo da ata".getBytes()
        );

        LembreteResponse response = lembreteService.inserirAta(100L, arquivo, EMAIL_CRIADOR);

        assertTrue(response.temAta());
        assertEquals("https://bucket.s3.amazonaws.com/atas/100/ata.pdf", response.ataUrl());
        assertEquals("ata_reuniao.pdf", response.ataNomeArquivo());
    }

    @Test
    @DisplayName("CA 6: Deve rejeitar inserção de ata se o lembrete não for do tipo Reunião")
    void testInserirAtaEmLembreteNaoReuniaoLancaErro() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.EDITAL, LocalDateTime.now().plusDays(10));

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "ata.pdf",
                "application/pdf",
                "bytes".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> lembreteService.inserirAta(100L, arquivo, EMAIL_CRIADOR));
    }

    @Test
    @DisplayName("CA 6: Deve rejeitar inserção de ata por usuário que não seja o criador")
    void testInserirAtaPorNaoCriadorLancaSemPermissao() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(1));

        when(usuarioService.findByEmail(EMAIL_OUTRO)).thenReturn(Optional.of(outroUsuario));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "ata.pdf",
                "application/pdf",
                "bytes".getBytes()
        );

        assertThrows(SemPermissaoNoGrupoErro.class, () -> lembreteService.inserirAta(100L, arquivo, EMAIL_OUTRO));
    }

    @Test
    @DisplayName("CA 1: Deve inativar lembrete com sucesso quando solicitado pelo criador")
    void testDeletarLembretePorCriadorComSucesso() {
        Evento existente = criarEventoExemplo(100L, TipoLembrete.WORKSHOP, LocalDateTime.now().plusDays(2));

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(100L)).thenReturn(Optional.of(existente));

        lembreteService.deletar(100L, EMAIL_CRIADOR);

        assertFalse(existente.isAtivo());
        verify(eventoRepository).save(existente);
    }

    @Test
    @DisplayName("CA 1: Deve listar lembretes do grupo paginados")
    void testListarPorGrupoComPaginacao() {
        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));

        Evento l1 = criarEventoExemplo(1L, TipoLembrete.WORKSHOP, LocalDateTime.now().plusDays(1));

        when(eventoRepository.filtrarPorGrupo(eq(10L), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(l1)));

        LembretePaginadoResponse response = lembreteService.listarPorGrupo(10L, null, null, 1, 10, EMAIL_CRIADOR);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(1, response.totalElements());
    }

    @Test
    @DisplayName("Paginação em ordem cronológica: futuros crescentes, depois passados do mais recente ao mais antigo")
    void testListarPorGrupoOrdemCronologicaEPaginas() {
        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));

        Evento longe = criarEventoExemplo(1L, TipoLembrete.EDITAL, LocalDateTime.now().plusDays(10));
        Evento passadoAntigo = criarEventoExemplo(2L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(20));
        Evento perto = criarEventoExemplo(3L, TipoLembrete.WORKSHOP, LocalDateTime.now().plusDays(1));
        Evento passadoRecente = criarEventoExemplo(4L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(2));

        when(eventoRepository.filtrarPorGrupo(eq(10L), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(longe, passadoAntigo, perto, passadoRecente)));

        LembretePaginadoResponse p1 = lembreteService.listarPorGrupo(10L, null, null, 1, 3, EMAIL_CRIADOR);
        LembretePaginadoResponse p2 = lembreteService.listarPorGrupo(10L, null, null, 2, 3, EMAIL_CRIADOR);

        assertEquals(List.of(3L, 1L, 4L), p1.items().stream().map(LembreteResponse::id).toList());
        assertEquals(List.of(2L), p2.items().stream().map(LembreteResponse::id).toList());
        assertEquals(4, p1.totalElements());
        assertEquals(2, p1.totalPages());
        assertEquals(2L, p1.totaisPorTipo().get("Reunião"));
    }

    @Test
    @DisplayName("Deve exportar os lembretes ativos do grupo em arquivo .ics")
    void testExportarIcs() {
        Evento evento = criarEventoExemplo(1L, TipoLembrete.REUNIAO, LocalDateTime.of(2026, 11, 10, 14, 0));
        evento.setRecorrencia(TipoRecorrencia.SEMANAL);

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(grupoRepository.findById(10L)).thenReturn(Optional.of(grupo));
        when(eventoRepository.findAllByGrupoIdAndIsAtivoTrueOrderByDthInicioAsc(10L))
                .thenReturn(List.of(evento));

        byte[] bytes = lembreteService.exportarIcs(10L, EMAIL_CRIADOR);
        String ics = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(ics.contains("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("X-WR-CALNAME:Grupo de Inteligência Artificial"));
        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("SUMMARY:Lembrete de Teste"));
        assertTrue(ics.contains("DTSTART:20261110T140000"));
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY"));
        assertTrue(ics.contains("END:VCALENDAR"));

        verify(grupoAcessoService).validarMembro(10L, 1L);
    }

    @Test
    @DisplayName("Deve confirmar presença de usuário logado no lembrete")
    void testConfirmarPresenca() {
        Evento l = criarEventoExemplo(1L, TipoLembrete.REUNIAO, LocalDateTime.now().plusDays(2));

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(1L)).thenReturn(Optional.of(l));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        LembreteResponse response = lembreteService.confirmarPresenca(1L, EMAIL_CRIADOR);

        assertTrue(response.presencaConfirmada());
        assertEquals(1, response.participantes().size());
    }

    @Test
    @DisplayName("CA 2: Validação e desserialização flexível dos tipos de lembrete")
    void testTiposLembreteDeserializacao() {
        assertEquals(TipoLembrete.EDITAL, TipoLembrete.fromString("Edital"));
        assertEquals(TipoLembrete.EDITAL, TipoLembrete.fromString("EDITAL"));
        assertEquals(TipoLembrete.REUNIAO, TipoLembrete.fromString("Reunião"));
        assertEquals(TipoLembrete.REUNIAO, TipoLembrete.fromString("reuniao"));
        assertEquals(TipoLembrete.APRESENTACAO, TipoLembrete.fromString("Apresentação"));
        assertEquals(TipoLembrete.APRESENTACAO, TipoLembrete.fromString("apresentacao"));
        assertEquals(TipoLembrete.WORKSHOP, TipoLembrete.fromString("Workshop"));
        assertEquals(TipoLembrete.WORKSHOP, TipoLembrete.fromString("WORKSHOP"));

        assertThrows(IllegalArgumentException.class, () -> TipoLembrete.fromString("Invalido"));
    }

    @Test
    @DisplayName("CA 5: Validação e desserialização dos tipos de recorrência")
    void testTipoRecorrenciaDeserializacao() {
        assertEquals(TipoRecorrencia.NENHUMA, TipoRecorrencia.fromString("Nenhuma"));
        assertEquals(TipoRecorrencia.DIARIA, TipoRecorrencia.fromString("Diária"));
        assertEquals(TipoRecorrencia.DIARIA, TipoRecorrencia.fromString("diaria"));
        assertEquals(TipoRecorrencia.SEMANAL, TipoRecorrencia.fromString("Semanal"));
        assertEquals(TipoRecorrencia.MENSAL, TipoRecorrencia.fromString("Mensal"));
        assertEquals(TipoRecorrencia.ANUAL, TipoRecorrencia.fromString("Anual"));
        assertEquals(TipoRecorrencia.NENHUMA, TipoRecorrencia.fromString(null));

        assertThrows(IllegalArgumentException.class, () -> TipoRecorrencia.fromString("Trimestral"));
    }

    @Test
    @DisplayName("Cálculo dinâmico de status: Encerrado (passado), Próximo (<= 3 dias), Agendado (> 3 dias)")
    void testStatusLembreteCalculo() {
        assertEquals(StatusLembrete.ENCERRADO, StatusLembrete.calcularStatus(LocalDateTime.now().minusHours(1)));
        assertEquals(StatusLembrete.PROXIMO, StatusLembrete.calcularStatus(LocalDateTime.now().plusDays(1)));
        assertEquals(StatusLembrete.PROXIMO, StatusLembrete.calcularStatus(LocalDateTime.now().plusDays(2)));
        assertEquals(StatusLembrete.AGENDADO, StatusLembrete.calcularStatus(LocalDateTime.now().plusDays(5)));
    }

    @Test
    @DisplayName("Download de ata com sucesso")
    void testDownloadAtaComSucesso() {
        Evento l = criarEventoExemplo(1L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(1));
        l.setAtaS3Key("atas/1/uuid_ata.pdf");
        l.setAtaUrl("https://s3/ata.pdf");

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(1L)).thenReturn(Optional.of(l));
        byte[] expectedBytes = "conteudo pdf".getBytes();
        when(s3StorageService.downloadFile("atas/1/uuid_ata.pdf")).thenReturn(expectedBytes);

        byte[] result = lembreteService.downloadAta(1L, EMAIL_CRIADOR);

        assertArrayEquals(expectedBytes, result);
    }

    @Test
    @DisplayName("Download de ata quando lembrete não possui ata deve lançar erro")
    void testDownloadAtaSemArquivoLancaErro() {
        Evento l = criarEventoExemplo(1L, TipoLembrete.REUNIAO, LocalDateTime.now().minusDays(1));

        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        when(eventoRepository.findByIdComRelacionamentos(1L)).thenReturn(Optional.of(l));

        assertThrows(IllegalArgumentException.class, () -> lembreteService.downloadAta(1L, EMAIL_CRIADOR));
    }

    @Test
    @DisplayName("Listar todos por grupo")
    void testListarTodosPorGrupo() {
        when(usuarioService.findByEmail(EMAIL_CRIADOR)).thenReturn(Optional.of(criador));
        Evento l = criarEventoExemplo(1L, TipoLembrete.REUNIAO, LocalDateTime.now().plusDays(1));
        when(eventoRepository.filtrarPorGrupo(eq(10L), eq(""), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(l)));

        List<LembreteResponse> list = lembreteService.listarTodosPorGrupo(10L, null, null, EMAIL_CRIADOR);

        assertEquals(1, list.size());
        assertEquals("Lembrete de Teste", list.get(0).titulo());
    }
}
