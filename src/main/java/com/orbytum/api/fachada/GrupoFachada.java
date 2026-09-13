package com.orbytum.api.fachada;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.request.CreateLeaderRequest;
import com.orbytum.api.models.dto.request.EditGroupRequest;
import com.orbytum.api.models.dto.request.EditLeaderRequest;
import com.orbytum.api.models.dto.response.GrupoPaginadoResponse;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.dto.response.LiderResponse;
import com.orbytum.api.models.dto.response.PesquisadorResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.service.ConviteService;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.UsuarioService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GrupoFachada {

    private static final Logger logger = LoggerFactory.getLogger(GrupoFachada.class);

    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final CredenciaisLoginService credenciaisLoginService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConviteService conviteService;

    @Transactional
    public GrupoResponse criarGrupo(CreateGroupRequest request) {
        if (grupoService.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe um grupo de pesquisa cadastrado com este nome");
        }

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario adminCriador = usuarioService.findByEmail(emailAdminLogado).orElse(null);

        Usuario usuarioLider = null;
        if (request.emailLider() != null && !request.emailLider().isBlank()) {
            String emailLider = request.emailLider().trim();
            usuarioLider = usuarioService.findByEmail(emailLider)
                    .orElseThrow(() -> new IllegalArgumentException("Não foi encontrado nenhum usuário cadastrado no sistema com o e-mail informado: " + emailLider));
        }

        Grupo novoGrupo = new Grupo(request.nome(), adminCriador);
        Grupo grupoSalvo = grupoService.save(novoGrupo);

        if (usuarioLider != null) {

            if(usuarioLider.getCredenciaisLogin().getAccessLevel() != AccessLevel.USER) {
                throw new IllegalArgumentException("O usuário informado não pode ser um administrador.");
            }

            Role roleLider = roleRepository.findFirstByIsLiderTrue()
                    .orElseGet(() -> roleRepository.save(new Role("Líder", List.of(), true)));

            if (!grupoXUsuarioRepository.existsByGrupoAndUsuario(grupoSalvo, usuarioLider)) {
                grupoXUsuarioRepository.save(new GrupoXUsuario(grupoSalvo, usuarioLider, roleLider, true));
            }
        }

        return mapearParaGrupoResponse(grupoSalvo);
    }

    @Transactional
    public GrupoResponse atualizarGrupo(Long id, EditGroupRequest request) {
        Grupo grupo = grupoService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de pesquisa não encontrado com ID: " + id));

        validarPermissaoAdminCriador(grupo);

        if (!grupo.getNome().equalsIgnoreCase(request.nome()) && grupoService.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe outro grupo de pesquisa cadastrado com este nome");
        }

        grupo.setNome(request.nome());
        if (request.isAtivo() != null) {
            grupo.setAtivo(request.isAtivo());
        }

        Grupo grupoAtualizado = grupoService.save(grupo);

        return mapearParaGrupoResponse(grupoAtualizado);
    }

    @Transactional
    public LiderResponse cadastrarLider(Long grupoId, CreateLeaderRequest request) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de pesquisa não encontrado com ID: " + grupoId));

        validarPermissaoAdminCriador(grupo);

        if (credenciaisLoginService.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail");
        }

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario adminCriador = usuarioService.findByEmail(emailAdminLogado).orElse(null);

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                request.telefone(),
                request.titulo());
        usuario = usuarioService.save(usuario);

        CredenciaisLogin credenciais = new CredenciaisLogin(
                request.email(),
                passwordEncoder.encode(request.senha()),
                AccessLevel.USER,
                usuario,
                adminCriador);
        credenciaisLoginService.save(credenciais);

        Role roleLider = roleRepository.findFirstByIsLiderTrue()
                .orElseGet(() -> roleRepository.save(new Role("Líder", List.of(), true)));

        GrupoXUsuario vinculo = new GrupoXUsuario(grupo, usuario, roleLider);
        grupoXUsuarioRepository.save(vinculo);

        return new LiderResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getTitulo(),
                grupo.getId(),
                true);
    }

    @Transactional
    public LiderResponse atualizarLider(Long grupoId, Long usuarioId, EditLeaderRequest request) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + grupoId));

        validarPermissaoAdminCriador(grupo);

        Usuario usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + usuarioId));

        if (!grupoXUsuarioRepository.existsByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuarioId)) {
            throw new IllegalArgumentException("Este usuário não está vinculado como líder deste grupo de pesquisa");
        }

        usuario.setNome(request.nome());
        usuario.setTelefone(request.telefone());
        usuario.setTitulo(request.titulo());

        Usuario usuarioAtualizado = usuarioService.save(usuario);

        return new LiderResponse(
                usuarioAtualizado.getId(),
                usuarioAtualizado.getNome(),
                usuarioAtualizado.getEmail(),
                usuarioAtualizado.getTelefone(),
                usuarioAtualizado.getTitulo(),
                grupo.getId(),
                true);
    }

    public GrupoPaginadoResponse listarGrupos(int page, int size, String nome, String usuario) {
        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        if (adminLogado.getAccessLevel() != AccessLevel.ADMIN) {
            throw new AccessDeniedException("Apenas Administradores possuem permissão para listar grupos");
        }

        Page<Grupo> pageResult = grupoService.listarGrupos(adminLogado, page, size, nome, usuario);

        List<GrupoResponse> items = pageResult.getContent().stream()
                .map(this::mapearParaGrupoResponse)
                .toList();

        return new GrupoPaginadoResponse(
                items,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                page,
                size
        );
    }

    public GrupoResponse buscarPorId(Long id) {
        Grupo grupo = grupoService.findById(id)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + id));

        validarPermissaoAdminCriador(grupo);

        return mapearParaGrupoResponse(grupo);
    }

    @Transactional
    public void removerGrupo(Long id) {
        Grupo grupo = grupoService.findById(id)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + id));

        validarPermissaoAdminCriador(grupo);

        grupo.setAtivo(false);
        grupoService.save(grupo);
    }

    public List<PesquisadorResponse> listarPesquisadores(Long grupoId) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + grupoId));

        validarPermissaoAdminCriador(grupo);

        List<GrupoXUsuario> vinculos = grupoXUsuarioRepository.findAllByGrupoIdAndIsAtivoTrue(grupoId);

        return vinculos.stream()
                .map(v -> new PesquisadorResponse(
                        v.getUsuario().getId(),
                        v.getUsuario().getNome(),
                        v.getUsuario().getEmail(),
                        v.getUsuario().getTelefone(),
                        v.getUsuario().getTitulo(),
                        grupo.getId(),
                        v.getRole() != null ? v.getRole().getNome() : "Membro",
                        v.getRole() != null && v.getRole().isLider()
                ))
                .toList();
    }

    @Transactional
    public PesquisadorResponse atualizarPesquisador(Long grupoId, Long usuarioId, EditLeaderRequest request) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + grupoId));

        validarPermissaoAdminCriador(grupo);

        Usuario usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + usuarioId));

        GrupoXUsuario vinculo = grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Este usuário não está vinculado a este grupo de pesquisa"));

        usuario.setNome(request.nome());
        usuario.setTelefone(request.telefone());
        usuario.setTitulo(request.titulo());
        Usuario usuarioAtualizado = usuarioService.save(usuario);

        return new PesquisadorResponse(
                usuarioAtualizado.getId(),
                usuarioAtualizado.getNome(),
                usuarioAtualizado.getEmail(),
                usuarioAtualizado.getTelefone(),
                usuarioAtualizado.getTitulo(),
                grupo.getId(),
                vinculo.getRole() != null ? vinculo.getRole().getNome() : "Membro",
                vinculo.getRole() != null && vinculo.getRole().isLider()
        );
    }

    @Transactional
    public void removerPesquisador(Long grupoId, Long usuarioId) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + grupoId));

        validarPermissaoAdminCriador(grupo);

        GrupoXUsuario vinculo = grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Este usuário não está vinculado a este grupo de pesquisa"));

        vinculo.setAtivo(false);
        grupoXUsuarioRepository.save(vinculo);
    }

    private GrupoResponse mapearParaGrupoResponse(Grupo grupo) {
        List<GrupoXUsuario> vinculos = grupoXUsuarioRepository.findAllByGrupoIdAndIsAtivoTrue(grupo.getId());
        int totalParticipantes = vinculos.size();

        GrupoXUsuario vinculoLider = vinculos.stream()
                .filter(v -> v.getRole() != null && v.getRole().isLider())
                .findFirst()
                .orElse(null);

        String nomeLider = vinculoLider != null && vinculoLider.getUsuario() != null ? vinculoLider.getUsuario().getNome() : null;
        Long idLider = vinculoLider != null && vinculoLider.getUsuario() != null ? vinculoLider.getUsuario().getId() : null;

        return new GrupoResponse(
                grupo.getId(),
                grupo.getNome(),
                grupo.isAtivo(),
                nomeLider,
                idLider,
                totalParticipantes
        );
    }

    private void validarPermissaoAdminCriador(Grupo grupo) {
        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        if (adminLogado.getAccessLevel() != AccessLevel.ADMIN) {
            throw new AccessDeniedException("Apenas o administrador que criou o grupo possui permissão para esta operação");
        }

        boolean isAdminCriador = grupo.getCriador() != null
                && grupo.getCriador().getEmail().equalsIgnoreCase(emailAdminLogado);

        if (!isAdminCriador) {
            throw new AccessDeniedException("Apenas o administrador que criou o grupo possui permissão para esta operação");
        }
    }
}
