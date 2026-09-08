package com.orbytum.api;

import com.orbytum.api.models.dto.response.ConviteCadastroPaginadoResponse;
import com.orbytum.api.models.entity.ConviteCadastro;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.SemPermissaoConvidarErro;
import com.orbytum.api.repository.ConviteCadastroRepository;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.ConviteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ConvitePaginacaoIntegrationTest {

    @Autowired
    private ConviteService conviteService;

    @Autowired
    private ConviteCadastroRepository conviteCadastroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CredenciaisLoginRepository credenciaisLoginRepository;

    private Usuario adminUser;
    private Usuario normalUser;

    @BeforeEach
    void setUp() {
        conviteCadastroRepository.deleteAll();

        adminUser = new Usuario("Admin Teste", "admin@teste.com", "11999999999", "Admin");
        adminUser = usuarioRepository.save(adminUser);
        CredenciaisLogin credAdmin = new CredenciaisLogin(
                adminUser.getEmail(), "senha123", AccessLevel.ADMIN, adminUser, null
        );
        credAdmin = credenciaisLoginRepository.save(credAdmin);
        // Refresh adminUser reference
        adminUser = credAdmin.getUsuario();

        normalUser = new Usuario("Normal Teste", "user@teste.com", "11888888888", "User");
        normalUser = usuarioRepository.save(normalUser);
        CredenciaisLogin credNormal = new CredenciaisLogin(
                normalUser.getEmail(), "senha123", AccessLevel.USER, normalUser, null
        );
        credNormal = credenciaisLoginRepository.save(credNormal);
        normalUser = credNormal.getUsuario();

        LocalDateTime now = LocalDateTime.now();

        // Convite 1: Ativo e não expirado (alice@empresa.com)
        ConviteCadastro c1 = new ConviteCadastro();
        c1.setEmail("alice@empresa.com");
        c1.setToken(UUID.randomUUID().toString());
        c1.setAtivo(true);
        c1.setDthRegistro(now.minusDays(1));
        c1.setDthExpiracao(now.plusDays(6));
        conviteCadastroRepository.save(c1);

        // Convite 2: Ativo e não expirado (bob@gmail.com)
        ConviteCadastro c2 = new ConviteCadastro();
        c2.setEmail("bob@gmail.com");
        c2.setToken(UUID.randomUUID().toString());
        c2.setAtivo(true);
        c2.setDthRegistro(now.minusDays(2));
        c2.setDthExpiracao(now.plusDays(5));
        conviteCadastroRepository.save(c2);

        // Convite 3: Inativo manualmente (charlie@empresa.com, isAtivo=false)
        ConviteCadastro c3 = new ConviteCadastro();
        c3.setEmail("charlie@empresa.com");
        c3.setToken(UUID.randomUUID().toString());
        c3.setAtivo(false);
        c3.setDthRegistro(now.minusDays(3));
        c3.setDthExpiracao(now.plusDays(4));
        conviteCadastroRepository.save(c3);

        // Convite 4: Expirado pelo tempo (daniel@empresa.com, isAtivo=true, expirado)
        ConviteCadastro c4 = new ConviteCadastro();
        c4.setEmail("daniel@empresa.com");
        c4.setToken(UUID.randomUUID().toString());
        c4.setAtivo(true);
        c4.setDthRegistro(now.minusDays(10));
        c4.setDthExpiracao(now.minusDays(3));
        conviteCadastroRepository.save(c4);
    }

    @Test
    void testApenasAdminPodeListar() {
        assertThrows(SemPermissaoConvidarErro.class, () -> {
            conviteService.listarConvitesCadastro(normalUser, 1, 5, null, "ativos");
        });
    }

    @Test
    void testListagemPadraoRetornaApenasAtivosNaoExpirados() {
        ConviteCadastroPaginadoResponse response = conviteService.listarConvitesCadastro(
                adminUser, 1, 5, null, "ativos"
        );

        assertNotNull(response);
        assertEquals(2, response.totalElements(), "Deve encontrar 2 ativos válidos");
        assertEquals(2, response.totalAtivos());
        assertEquals(2, response.totalInativos());
        assertEquals(4, response.totalGeral());
        assertEquals(2, response.items().size());
        assertTrue(response.items().stream().allMatch(c -> c.ativo()));
    }

    @Test
    void testFiltroStatusInativosRetornaRevogadosEExpirados() {
        ConviteCadastroPaginadoResponse response = conviteService.listarConvitesCadastro(
                adminUser, 1, 5, null, "inativos"
        );

        assertNotNull(response);
        assertEquals(2, response.totalElements(), "Deve conter 2 inativos (1 revogado + 1 expirado)");
        assertEquals(2, response.items().size());
        assertTrue(response.items().stream().noneMatch(c -> c.ativo()));
    }

    @Test
    void testFiltroStatusTodosRetornaTodosOsConvites() {
        ConviteCadastroPaginadoResponse response = conviteService.listarConvitesCadastro(
                adminUser, 1, 10, null, "todos"
        );

        assertNotNull(response);
        assertEquals(4, response.totalElements());
        assertEquals(4, response.items().size());
    }

    @Test
    void testFiltroPorEmailComCaseInsensitiveESubstring() {
        ConviteCadastroPaginadoResponse response = conviteService.listarConvitesCadastro(
                adminUser, 1, 5, "ALICE", "ativos"
        );

        assertEquals(1, response.totalElements());
        assertEquals("alice@empresa.com", response.items().get(0).email());

        // Empresa retorna alice (já que bob é gmail, charlie e daniel são inativos e status=ativos)
        ConviteCadastroPaginadoResponse respEmpresa = conviteService.listarConvitesCadastro(
                adminUser, 1, 5, "empresa", "ativos"
        );
        assertEquals(1, respEmpresa.totalElements());
        assertEquals("alice@empresa.com", respEmpresa.items().get(0).email());

        // Empresa com status todos retorna 3 (alice, charlie, daniel)
        ConviteCadastroPaginadoResponse respEmpresaTodos = conviteService.listarConvitesCadastro(
                adminUser, 1, 5, "empresa", "todos"
        );
        assertEquals(3, respEmpresaTodos.totalElements());
    }

    @Test
    void testPaginacaoBackend() {
        // Página 1 com tamanho 1
        ConviteCadastroPaginadoResponse p1 = conviteService.listarConvitesCadastro(
                adminUser, 1, 1, null, "ativos"
        );
        assertEquals(1, p1.items().size());
        assertEquals(2, p1.totalElements());
        assertEquals(2, p1.totalPages());
        assertEquals(1, p1.currentPage());
        assertEquals(1, p1.pageSize());

        // Página 2 com tamanho 1
        ConviteCadastroPaginadoResponse p2 = conviteService.listarConvitesCadastro(
                adminUser, 2, 1, null, "ativos"
        );
        assertEquals(1, p2.items().size());
        assertEquals(2, p2.currentPage());
        assertNotEquals(p1.items().get(0).id(), p2.items().get(0).id());
    }
}
