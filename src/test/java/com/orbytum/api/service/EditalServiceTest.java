package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CriarEditalRequest;
import com.orbytum.api.models.dto.response.EditalPaginadoResponse;
import com.orbytum.api.models.dto.response.EditalResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Edital;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.EditalXGrupo;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.EditalJaVinculadoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.EditalRepository;
import com.orbytum.api.repository.EditalXGrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditalServiceTest {

    private static final String EMAIL = "coordenador@test.com";
    private static final String URI = "https://cnpq.br/edital/1";

    @Mock
    private EditalRepository editalRepository;

    @Mock
    private EditalXGrupoRepository editalXGrupoRepository;

    @Mock
    private GrupoService grupoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GrupoAcessoService grupoAcessoService;

    @InjectMocks
    private EditalService editalService;

    private Grupo grupo;
    private Usuario coordenador;

    @BeforeEach
    void setUp() {
        grupo = new Grupo("Grupo Editais", true);
        grupo.setId(1L);
        coordenador = usuario(20L, "Coordenador", EMAIL, AccessLevel.USER);
    }

    private Usuario usuario(Long id, String nome, String email, AccessLevel nivel) {
        CredenciaisLogin credenciais = new CredenciaisLogin(email, "hash", nivel, new Usuario(), null);
        return new Usuario(id, nome, email, "11999999999", "Dr", LocalDateTime.now(), true, credenciais);
    }

    private CriarEditalRequest request(LocalDateTime abertura, LocalDateTime encerramento) {
        return new CriarEditalRequest("CNPq", "Edital de pesquisa em tecnologia",
                new BigDecimal("50000.00"), URI, abertura, encerramento);
    }

    @Test
    void criarEditalComoCoordenadorVinculaAoGrupo() {
        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(editalRepository.findByUri(URI)).thenReturn(Optional.empty());
        when(editalRepository.save(any(Edital.class))).thenAnswer(inv -> {
            Edital edital = inv.getArgument(0);
            edital.setId(7L);
            return edital;
        });
        when(editalXGrupoRepository.existsByEditalIdAndGrupoId(7L, 1L)).thenReturn(false);
        when(editalXGrupoRepository.save(any(EditalXGrupo.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime abertura = LocalDateTime.now();
        EditalResponse response = editalService.criar(1L, request(abertura, abertura.plusDays(30)), EMAIL);

        assertEquals(7L, response.id());
        assertEquals("CNPq", response.organizador());
        verify(grupoAcessoService).validarCoordenadorOuAcima(1L, 20L);
        verify(editalXGrupoRepository).save(any(EditalXGrupo.class));
    }

    @Test
    void criarEditalSemPermissaoLancaErro() {
        Usuario pesquisador = usuario(10L, "Pesquisador", "pesquisa@test.com", AccessLevel.USER);
        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail("pesquisa@test.com")).thenReturn(Optional.of(pesquisador));
        doThrow(new SemPermissaoNoGrupoErro("Sem permissão"))
                .when(grupoAcessoService).validarCoordenadorOuAcima(1L, 10L);

        LocalDateTime abertura = LocalDateTime.now();
        assertThrows(SemPermissaoNoGrupoErro.class,
                () -> editalService.criar(1L, request(abertura, abertura.plusDays(30)), "pesquisa@test.com"));
    }

    @Test
    void periodoInvalidoLancaErro() {
        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));

        LocalDateTime abertura = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,
                () -> editalService.criar(1L, request(abertura, abertura.minusDays(1)), EMAIL));
    }

    @Test
    void editalJaVinculadoLancaErro() {
        Edital existente = Edital.builder()
                .id(7L)
                .organizador("CNPq")
                .descricao("Edital existente")
                .uri(URI)
                .dthAbertura(LocalDateTime.now())
                .dthEncerramento(LocalDateTime.now().plusDays(10))
                .build();

        when(grupoService.findById(1L)).thenReturn(Optional.of(grupo));
        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(editalRepository.findByUri(URI)).thenReturn(Optional.of(existente));
        when(editalXGrupoRepository.existsByEditalIdAndGrupoId(7L, 1L)).thenReturn(true);

        LocalDateTime abertura = LocalDateTime.now();
        assertThrows(EditalJaVinculadoErro.class,
                () -> editalService.criar(1L, request(abertura, abertura.plusDays(30)), EMAIL));
    }

    @Test
    void listarNormalizaFiltrosEPagina() {
        Edital edital = Edital.builder()
                .id(7L)
                .organizador("CNPq")
                .descricao("Edital de tecnologia")
                .uri(URI)
                .dthAbertura(LocalDateTime.now())
                .dthEncerramento(LocalDateTime.now().plusDays(10))
                .build();

        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(editalRepository.filtrarPorGrupo(eq(1L), eq("Tecnologia"), eq("CNPq"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(edital)));

        EditalPaginadoResponse response = editalService.listar(1L, "  Tecnologia  ", "  CNPq  ", null, null, 1, 10, EMAIL);

        assertEquals(1, response.items().size());
        assertEquals("CNPq", response.items().get(0).organizador());
        verify(grupoAcessoService).validarMembro(1L, 20L);
    }

    @Test
    void removerUltimoVinculoApagaEdital() {
        Edital edital = Edital.builder()
                .id(7L)
                .organizador("CNPq")
                .descricao("Edital")
                .uri("https://a.com/1")
                .dthAbertura(LocalDateTime.now())
                .dthEncerramento(LocalDateTime.now().plusDays(1))
                .build();
        EditalXGrupo vinculo = new EditalXGrupo(edital, grupo);

        when(usuarioService.findByEmail(EMAIL)).thenReturn(Optional.of(coordenador));
        when(editalRepository.findById(7L)).thenReturn(Optional.of(edital));
        when(editalXGrupoRepository.existsByEditalIdAndGrupoId(7L, 1L)).thenReturn(true);
        when(editalXGrupoRepository.findByEditalIdAndGrupoId(7L, 1L)).thenReturn(Optional.of(vinculo));
        when(editalXGrupoRepository.countByEditalId(7L)).thenReturn(1L);

        editalService.removerDoGrupo(7L, 1L, EMAIL);

        verify(editalXGrupoRepository).delete(vinculo);
        verify(editalRepository).delete(edital);
    }
}
