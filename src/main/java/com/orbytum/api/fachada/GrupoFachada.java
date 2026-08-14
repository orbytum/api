package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.request.CreateLeaderRequest;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.dto.response.LiderResponse;
import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.UsuarioService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

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

        Grupo novoGrupo = new Grupo(request.nome());
        Grupo grupoSalvo = grupoService.save(novoGrupo);

        return new GrupoResponse(grupoSalvo.getId(), grupoSalvo.getNome(), grupoSalvo.isAtivo());
    }

    @Transactional
    public LiderResponse cadastrarLider(Long grupoId, CreateLeaderRequest request) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de pesquisa não encontrado com ID: " + grupoId));

        if (credenciaisLoginService.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail");
        }

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
                null);
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

    public List<GrupoResponse> listarGrupos() {
        return grupoService.findAllAtivos()
                .stream()
                .map(grupo -> new GrupoResponse(
                        grupo.getId(),
                        grupo.getNome(),
                        grupo.isAtivo()))
                .toList();
    }
}
