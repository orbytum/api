package com.orbytum.api.fachada;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.request.CreateLeaderRequest;
import com.orbytum.api.models.dto.request.EditGroupRequest;
import com.orbytum.api.models.dto.request.EditLeaderRequest;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.dto.response.LiderResponse;
import com.orbytum.api.models.dto.response.PesquisadorResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.UsuarioService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GrupoFachada {
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final CredenciaisLoginService credenciaisLoginService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public GrupoResponse criarGrupo(CreateGroupRequest request) {
        if (grupoService.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe um grupo de pesquisa cadastrado com este nome");
        }

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario adminCriador = usuarioService.findByEmail(emailAdminLogado).orElse(null);

        Grupo novoGrupo = new Grupo(request.nome(), adminCriador);
        Grupo grupoSalvo = grupoService.save(novoGrupo);

        return new GrupoResponse(grupoSalvo.getId(), grupoSalvo.getNome(), grupoSalvo.isAtivo());
    }

    @Transactional
    public GrupoResponse atualizarGrupo(Long id, EditGroupRequest request) {
        Grupo grupo = grupoService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de pesquisa não encontrado com ID: " + id));

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não autenticado"));

        // permitindo que o admin inicial também edite
        boolean isInitialAdmin = adminLogado.getAccessLevel() == AccessLevel.INITIAL_ADMIN;
        boolean isAdminCriador = grupo.getCriador() != null
                && grupo.getCriador().getEmail().equalsIgnoreCase(emailAdminLogado);

        if (!isInitialAdmin && !isAdminCriador) {
            throw new AccessDeniedException("Apenas o administrador que criou o grupo pode editar");
        }

        if (!grupo.getNome().equalsIgnoreCase(request.nome()) && grupoService.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe outro grupo de pesquisa cadastrado com este nome");
        }

        grupo.setNome(request.nome());
        if (request.isAtivo() != null) {
            grupo.setAtivo(request.isAtivo());
        }

        Grupo grupoAtualizado = grupoService.save(grupo);

        return new GrupoResponse(grupoAtualizado.getId(), grupoAtualizado.getNome(), grupoAtualizado.isAtivo());
    }

    @Transactional
    public LiderResponse cadastrarLider(Long grupoId, CreateLeaderRequest request) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de pesquisa não encontrado com ID: " + grupoId));

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

        GrupoXUsuario vinculo = new GrupoXUsuario(grupo, usuario, null);
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
                .orElseThrow(() -> new IllegalArgumentException("Grupo de pesquisa não encontrado com ID: " + grupoId));

        Usuario usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + usuarioId));

        if (!grupoXUsuarioRepository.existsByGrupoAndUsuario(grupo, usuario)) {
            throw new IllegalArgumentException("Este usuário não está vinculado como líder deste grupo de pesquisa");
        }

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        CredenciaisLogin credenciaisLider = credenciaisLoginService.findByEmail(usuario.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais do líder não encontradas"));

        boolean isAdminInicial = adminLogado.getAccessLevel() == AccessLevel.INITIAL_ADMIN;
        boolean isAdminCriador = credenciaisLider.getCriador() != null
                && credenciaisLider.getCriador().getEmail().equalsIgnoreCase(emailAdminLogado);

        if (!isAdminInicial && !isAdminCriador) {
            throw new AccessDeniedException("Apenas o administrador que cadastrou o líder pode editar");
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

    public List<GrupoResponse> listarGrupos() {
        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        List<Grupo> grupos;
        if (adminLogado.getAccessLevel() == AccessLevel.INITIAL_ADMIN) {
            grupos = grupoService.findAllAtivos();
        } else {
            Usuario adminCriador = adminLogado.getUsuario();
            grupos = grupoService.findAllByCriador(adminCriador);
        }

        return grupos.stream()
                .map(grupo -> new GrupoResponse(
                        grupo.getId(),
                        grupo.getNome(),
                        grupo.isAtivo()))
                .toList();
    }

    public GrupoResponse buscarPorId(Long id) {
        Grupo grupo = grupoService.findById(id)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + id));

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        boolean isInitialAdmin = adminLogado.getAccessLevel() == AccessLevel.INITIAL_ADMIN;
        boolean isAdminCriador = grupo.getCriador() != null
                && grupo.getCriador().getEmail().equalsIgnoreCase(emailAdminLogado);

        if (!isInitialAdmin && !isAdminCriador) {
            throw new AccessDeniedException("Apenas o administrador que criou o grupo pode visualizar");
        }

        return new GrupoResponse(grupo.getId(), grupo.getNome(), grupo.isAtivo());
    }

    @Transactional
    public void removerGrupo(Long id) {
        Grupo grupo = grupoService.findById(id)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + id));

        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        boolean isInitialAdmin = adminLogado.getAccessLevel() == AccessLevel.INITIAL_ADMIN;
        boolean isAdminCriador = grupo.getCriador() != null
                && grupo.getCriador().getEmail().equalsIgnoreCase(emailAdminLogado);

        if (!isInitialAdmin && !isAdminCriador) {
            throw new AccessDeniedException("Apenas o administrador que criou o grupo pode removê-lo");
        }

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

    private void validarPermissaoAdminCriador(Grupo grupo) {
        String emailAdminLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        CredenciaisLogin adminLogado = credenciaisLoginService.findByEmail(emailAdminLogado)
                .orElseThrow(() -> new AccessDeniedException("Administrador não autenticado"));

        boolean isInitialAdmin = adminLogado.getAccessLevel() == AccessLevel.INITIAL_ADMIN;
        boolean isAdminCriador = grupo.getCriador() != null
                && grupo.getCriador().getEmail().equalsIgnoreCase(emailAdminLogado);

        if (!isInitialAdmin && !isAdminCriador) {
            throw new AccessDeniedException("Apenas o administrador que criou o grupo possui permissão para esta operação");
        }
    }
}
