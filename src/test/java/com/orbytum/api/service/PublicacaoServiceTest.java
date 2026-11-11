package com.orbytum.api.service;

import com.orbytum.api.configuration.StorageProperties;
import com.orbytum.api.models.dto.request.CreatePublicacaoRequest;
import com.orbytum.api.models.dto.response.PublicacaoPaginadaResponse;
import com.orbytum.api.models.dto.response.PublicacaoResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Publicacao;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.StorageProvider;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.PublicacaoNaoEncontradaErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.PublicacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicacaoServiceTest {

    @Mock
    private PublicacaoRepository publicacaoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private ArquivoStorage arquivoStorage;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoAcessoService grupoAcessoService;

    @InjectMocks
    private PublicacaoService publicacaoService;

    private static final String EMAIL = "gestor@test.com";

    private Projeto projeto;
    private Usuario gestor;

    @BeforeEach
    void setUp() {
        Grupo grupo = new Grupo("Grupo Publicações", true);
        grupo.setId(1L);

        projeto = new Projeto();
        projeto.setId(10L);
        projeto.setTitulo("Projeto Inteligência Artificial");
        projeto.setGrupo(grupo);
        projeto.setAtivo(true);

        CredenciaisLogin credenciais = new CredenciaisLogin(
                EMAIL, "hash", AccessLevel.USER, new Usuario(), null);
        gestor = new Usuario(1L, "Gestor", EMAIL, "11999999999", "Dr",
                LocalDateTime.now(), true, credenciais);

        lenient().when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(gestor));
        lenient().when(grupoAcessoService.podeGerenciarProjeto(anyLong(), anyLong())).thenReturn(true);
        lenient().when(storageProperties.getPublicBaseUrl()).thenReturn("http://localhost:8080");
    }

    @Test
    void testCriarPublicacaoComSucesso() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "arquivo",
                "pesquisa.pdf",
                "application/pdf",
                "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.UTF_8)
        );

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título Artigo",
                "Descrição do Artigo Científico",
                10L,
                pdfFile
        );

        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(arquivoStorage.salvar(anyString(), any(MultipartFile.class)))
                .thenReturn(new ArquivoArmazenado(
                        StorageProvider.S3,
                        "publicacoes/random.pdf",
                        "https://test-bucket.s3.sa-east-1.amazonaws.com/publicacoes/random.pdf",
                        "pesquisa.pdf",
                        "application/pdf",
                        pdfFile.getSize(),
                        null
                ));

        when(publicacaoRepository.save(any(Publicacao.class))).thenAnswer(invocation -> {
            Publicacao p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PublicacaoResponse response = publicacaoService.criarPublicacao(request, EMAIL);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Título Artigo", response.titulo());
        assertEquals("Descrição do Artigo Científico", response.descricao());
        assertEquals(10L, response.projetoId());
        assertNotNull(response.uuid());
        assertTrue(response.isAtivo());
        assertTrue(response.url().contains("amazonaws.com"));

        ArgumentCaptor<String> chaveCaptor = ArgumentCaptor.forClass(String.class);
        verify(arquivoStorage).salvar(chaveCaptor.capture(), eq(pdfFile));
        assertTrue(chaveCaptor.getValue().startsWith("publicacoes/"));
        assertTrue(chaveCaptor.getValue().endsWith(".pdf"));

        verify(publicacaoRepository).save(any(Publicacao.class));
    }

    @Test
    void testCriarPublicacaoComFallbackNoBanco() {
        byte[] conteudo = "%PDF-1.4 fallback".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile pdfFile = new MockMultipartFile(
                "arquivo",
                "pesquisa.pdf",
                "application/pdf",
                conteudo
        );

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título Fallback",
                "Descrição Fallback",
                10L,
                pdfFile
        );

        Documento documento = Documento.builder()
                .id(99L)
                .nome("pesquisa.pdf")
                .extensao("pdf")
                .contentType("application/pdf")
                .tamanho((long) conteudo.length)
                .chave("publicacoes/fallback.pdf")
                .documento(conteudo)
                .build();

        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(arquivoStorage.salvar(anyString(), any(MultipartFile.class)))
                .thenReturn(new ArquivoArmazenado(
                        StorageProvider.DATABASE,
                        "publicacoes/fallback.pdf",
                        null,
                        "pesquisa.pdf",
                        "application/pdf",
                        conteudo.length,
                        documento
                ));

        when(publicacaoRepository.save(any(Publicacao.class))).thenAnswer(invocation -> {
            Publicacao p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        PublicacaoResponse response = publicacaoService.criarPublicacao(request, EMAIL);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("http://localhost:8080/publicacoes/uuid/" + response.uuid() + "/arquivo", response.url());

        ArgumentCaptor<Publicacao> captor = ArgumentCaptor.forClass(Publicacao.class);
        verify(publicacaoRepository).save(captor.capture());
        assertEquals(StorageProvider.DATABASE, captor.getValue().getStorageProvider());
        assertNotNull(captor.getValue().getDocumento());
        assertArrayEquals(conteudo, captor.getValue().getDocumento().getDocumento());
    }

    @Test
    void testPesquisadorNaoPodeCriarPublicacao() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "arquivo", "pesquisa.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título Artigo", "Descrição", 10L, pdfFile);

        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(grupoAcessoService.podeGerenciarProjeto(10L, 1L)).thenReturn(false);

        assertThrows(SemPermissaoNoGrupoErro.class,
                () -> publicacaoService.criarPublicacao(request, EMAIL));
        verifyNoInteractions(arquivoStorage);
    }

    @Test
    void testCriarPublicacaoRejeitaArquivoNaoPdfPelaExtensao() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "arquivo",
                "documento.txt",
                "text/plain",
                "conteudo de texto".getBytes(StandardCharsets.UTF_8)
        );

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título",
                "Descrição",
                10L,
                txtFile
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacaoService.criarPublicacao(request, EMAIL));

        assertEquals("A publicação deve ser exclusivamente no formato PDF.", ex.getMessage());
        verifyNoInteractions(arquivoStorage);
        verify(publicacaoRepository, never()).save(any());
    }

    @Test
    void testCriarPublicacaoRejeitaArquivoNaoPdfPeloContentType() {
        MockMultipartFile fileWithWrongType = new MockMultipartFile(
                "arquivo",
                "documento.pdf",
                "image/png",
                new byte[]{1, 2, 3}
        );

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título",
                "Descrição",
                10L,
                fileWithWrongType
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacaoService.criarPublicacao(request, EMAIL));

        assertEquals("A publicação deve ser exclusivamente no formato PDF.", ex.getMessage());
        verifyNoInteractions(arquivoStorage);
    }

    @Test
    void testCriarPublicacaoRejeitaArquivoVazio() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "arquivo",
                "documento.pdf",
                "application/pdf",
                new byte[0]
        );

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título",
                "Descrição",
                10L,
                emptyFile
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacaoService.criarPublicacao(request, EMAIL));

        assertEquals("O arquivo PDF da publicação é obrigatório.", ex.getMessage());
    }

    @Test
    void testCriarPublicacaoProjetoNaoEncontrado() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "arquivo",
                "artigo.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)
        );

        CreatePublicacaoRequest request = new CreatePublicacaoRequest(
                "Título",
                "Descrição",
                999L,
                pdfFile
        );

        when(projetoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProjetoNaoEncontradoErro.class, () -> publicacaoService.criarPublicacao(request, EMAIL));
        verifyNoInteractions(arquivoStorage);
        verify(publicacaoRepository, never()).save(any());
    }

    @Test
    void testListarPublicacoesComFiltros() {
        Publicacao p = Publicacao.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .titulo("Artigo A")
                .descricao("Desc A")
                .projeto(projeto)
                .url("https://s3.aws.com/publicacoes/1.pdf")
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();

        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(publicacaoRepository.filtrarPublicacoes(eq("Artigo"), any(), any(), eq(10L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        PublicacaoPaginadaResponse response = publicacaoService.listarPublicacoes(
                "Artigo",
                LocalDate.now().minusDays(10),
                LocalDate.now(),
                10L,
                null,
                1,
                10,
                EMAIL
        );

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals("Artigo A", response.items().get(0).titulo());
        assertEquals(1, response.totalElements());
    }

    @Test
    void testBuscarPorIdComSucesso() {
        Publicacao p = Publicacao.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .titulo("Artigo A")
                .descricao("Desc A")
                .projeto(projeto)
                .url("https://s3.aws.com/publicacoes/1.pdf")
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();

        when(publicacaoRepository.findByIdAndIsAtivoTrue(1L)).thenReturn(Optional.of(p));

        PublicacaoResponse response = publicacaoService.buscarPorId(1L, EMAIL);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    void testBuscarPorIdNaoEncontradoLancaExcecao() {
        when(publicacaoRepository.findByIdAndIsAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(PublicacaoNaoEncontradaErro.class, () -> publicacaoService.buscarPorId(99L, EMAIL));
    }

    @Test
    void testInativarPublicacaoComSucesso() {
        Publicacao p = Publicacao.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .titulo("Artigo A")
                .descricao("Desc A")
                .projeto(projeto)
                .url("https://s3.aws.com/publicacoes/1.pdf")
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();

        when(publicacaoRepository.findByIdAndIsAtivoTrue(1L)).thenReturn(Optional.of(p));

        publicacaoService.inativarPublicacao(1L, EMAIL);

        assertFalse(p.isAtivo());
        verify(publicacaoRepository).save(p);
    }

    @Test
    void testInativarPublicacaoInexistenteLancaErro() {
        when(publicacaoRepository.findByIdAndIsAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(PublicacaoNaoEncontradaErro.class, () -> publicacaoService.inativarPublicacao(99L, EMAIL));
        verify(publicacaoRepository, never()).save(any());
    }
}
