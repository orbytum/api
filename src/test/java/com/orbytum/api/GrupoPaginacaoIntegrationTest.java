package com.orbytum.api;

import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.repository.UsuarioRepository;
import com.orbytum.api.service.GrupoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GrupoPaginacaoIntegrationTest {

    @Autowired
    private GrupoService grupoService;

    @Autowired
    private com.orbytum.api.fachada.GrupoFachada grupoFachada;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CredenciaisLoginRepository credenciaisLoginRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GrupoXUsuarioRepository grupoXUsuarioRepository;

    private CredenciaisLogin initialAdminCreds;
    private CredenciaisLogin normalAdminCreds;
    private Usuario initialAdminUser;
    private Usuario normalAdminUser;

    @BeforeEach
    void setUp() {
        grupoXUsuarioRepository.deleteAll();
        grupoRepository.deleteAll();

        initialAdminUser = usuarioRepository.save(new Usuario("Admin Inicial", "initialadmin@teste.com", "11999990001", "Dr"));
        initialAdminCreds = credenciaisLoginRepository.save(new CredenciaisLogin(
                initialAdminUser.getEmail(), "senha123", AccessLevel.INITIAL_ADMIN, initialAdminUser, null
        ));

        normalAdminUser = usuarioRepository.save(new Usuario("Admin Comum", "admincomum@teste.com", "11999990002", "Prof"));
        normalAdminCreds = credenciaisLoginRepository.save(new CredenciaisLogin(
                normalAdminUser.getEmail(), "senha123", AccessLevel.ADMIN, normalAdminUser, null
        ));
    }

    @Test
    void testListarGruposComPaginacaoEFiltros() {
        Grupo g1 = grupoRepository.save(new Grupo("Grupo de Inteligência Artificial", normalAdminUser));
        Grupo g2 = grupoRepository.save(new Grupo("Grupo de Robótica Avançada", normalAdminUser));
        Grupo g3 = grupoRepository.save(new Grupo("Laboratório de Redes", normalAdminUser));

        Page<Grupo> paginaTodos = grupoService.listarGrupos(normalAdminCreds, 1, 10, null, null);
        assertEquals(3, paginaTodos.getTotalElements());
        assertEquals(1, paginaTodos.getTotalPages());

        Page<Grupo> paginaNome = grupoService.listarGrupos(normalAdminCreds, 1, 10, "Inteligência", null);
        assertEquals(1, paginaNome.getTotalElements());
        assertEquals("Grupo de Inteligência Artificial", paginaNome.getContent().get(0).getNome());
    }

    @Test
    void testInitialAdminNaoTemPermissaoParaListarGrupos() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(initialAdminUser.getEmail(), null, List.of())
        );

        assertThrows(AccessDeniedException.class, () -> {
            grupoFachada.listarGrupos(1, 10, null, null);
        });
    }

    @Test
    void testCriarGrupoComEmailLiderInexistenteLancaExcecao() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(normalAdminUser.getEmail(), null, List.of())
        );

        CreateGroupRequest request = new CreateGroupRequest("Grupo Teste Sem Lider", "naoexistente@empresa.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            grupoFachada.criarGrupo(request);
        });

        assertTrue(ex.getMessage().contains("Não foi encontrado nenhum usuário cadastrado no sistema"));
    }

    @Test
    void testCriarGrupoComEmailLiderExistenteVinculaLider() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(normalAdminUser.getEmail(), null, List.of())
        );

        Usuario liderExistente = usuarioRepository.save(new Usuario("Líder Cadastrado", "lider.existente@empresa.com", "11988889999", "Prof"));

        CreateGroupRequest request = new CreateGroupRequest("Grupo Teste Com Lider", liderExistente.getEmail());
        GrupoResponse response = grupoFachada.criarGrupo(request);

        assertNotNull(response);
        assertEquals("Grupo Teste Com Lider", response.nome());
        assertEquals("Líder Cadastrado", response.nomeLider());
        assertEquals(liderExistente.getId(), response.idLider());
        assertEquals(1, response.totalParticipantes());
    }
}
