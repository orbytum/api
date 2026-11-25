package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.EditUsuarioRequest;
import com.orbytum.api.models.dto.response.UsuarioPaginadoResponse;
import com.orbytum.api.models.dto.response.UsuarioResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.EmailJaCadastradoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CredenciaisLoginRepository credenciaisLoginRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, CredenciaisLoginRepository credenciaisLoginRepository) {
        this.usuarioRepository = usuarioRepository;
        this.credenciaisLoginRepository = credenciaisLoginRepository;
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional
    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void delete(Usuario usuario) {
        usuarioRepository.delete(usuario);
    }

    public long count() {
        return usuarioRepository.count();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    public UsuarioPaginadoResponse listar(int page, int size, String busca) {
        int pageSize = size > 0 ? size : 10;
        String termo = busca != null ? busca.trim() : "";
        Page<Usuario> resultado = usuarioRepository.buscar(termo,
                PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by("nome")));

        return new UsuarioPaginadoResponse(
                resultado.getContent().stream().map(this::toResponse).toList(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                page,
                pageSize);
    }

    /** Edita os dados cadastrais (senha não é alterada aqui). */
    @Transactional
    public UsuarioResponse atualizar(Long id, EditUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + id));

        String emailAtual = usuario.getEmail();
        String novoEmail = request.email().trim();

        if (!novoEmail.equalsIgnoreCase(emailAtual)) {
            boolean emUso = usuarioRepository.findByEmail(novoEmail).filter(u -> !u.getId().equals(id)).isPresent()
                    || credenciaisLoginRepository.existsByEmail(novoEmail);
            if (emUso) {
                throw new EmailJaCadastradoErro("Já existe um usuário cadastrado com este e-mail");
            }
            // O e-mail também é o login: credenciais e usuário precisam mudar juntos
            credenciaisLoginRepository.findByEmail(emailAtual).ifPresent(c -> {
                c.setEmail(novoEmail);
                credenciaisLoginRepository.save(c);
            });
            usuario.setEmail(novoEmail);
        }

        usuario.setNome(request.nome().trim());
        usuario.setTelefone(request.telefone().trim());
        usuario.setTitulo(request.titulo().trim());

        return toResponse(usuarioRepository.save(usuario));
    }

    /** Inativa o usuário e o login; o filtro JWT recusa credenciais inativas, então o acesso cai na hora. */
    @Transactional
    public void inativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + id));
        CredenciaisLogin credenciais = credenciaisLoginRepository.findByEmail(usuario.getEmail()).orElse(null);

        if (credenciais != null && credenciais.getAccessLevel() != AccessLevel.USER) {
            throw new IllegalArgumentException("Administradores não podem ser inativados por esta tela");
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        if (credenciais != null) {
            credenciais.setAtivo(false);
            credenciaisLoginRepository.save(credenciais);
        }
    }

    private UsuarioResponse toResponse(Usuario u) {
        CredenciaisLogin credenciais = credenciaisLoginRepository.findByEmail(u.getEmail()).orElse(null);
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getTelefone(),
                u.getTitulo(),
                credenciais != null ? credenciais.getAccessLevel() : null,
                u.isAtivo(),
                u.getDthRegistro());
    }
}
