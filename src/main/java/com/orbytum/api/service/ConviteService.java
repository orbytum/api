package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.GerarConviteCadastroRequest;
import com.orbytum.api.models.dto.request.GerarConviteGrupoRequest;
import com.orbytum.api.models.dto.response.ConviteCadastroResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoResponse;
import com.orbytum.api.models.entity.*;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.*;
import com.orbytum.api.repository.ConviteCadastroRepository;
import com.orbytum.api.repository.ConviteGrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ConviteService {

    private final ConviteGrupoRepository conviteGrupoRepository;
    private final ConviteCadastroRepository conviteCadastroRepository;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioService grupoXUsuarioService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final ProjetoService projetoService;
    private final RoleRepository roleRepository;

    @Transactional
    public ConviteGrupoEnviadoResponse enviarConviteGrupo(Usuario remetente, Long grupoId, String emailConvidado, List<Long> projetoIds) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + grupoId));

        validarPermissaoConviteGrupo(remetente, grupoId);

        Usuario convidado = usuarioService.findByEmail(emailConvidado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário convidado não encontrado com e-mail: " + emailConvidado));

        if (grupoXUsuarioService.isUsuarioNoGrupo(grupoId, convidado.getId())) {
            throw new UsuarioJaNoGrupoErro("O usuário convidado já pertence a este grupo");
        }

        List<Projeto> projetos = validarProjetos(grupoId, projetoIds);

        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(7);
        ConviteGrupo conviteGrupo = new ConviteGrupo(grupo, convidado, remetente, projetos, dthExpiracao);
        conviteGrupo = conviteRepository.save(conviteGrupo);

        List<Long> idsProjetosSalvos = conviteGrupo.getProjetos() != null
                ? conviteGrupo.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        return new ConviteGrupoEnviadoResponse(
                conviteGrupo.getId(),
                grupo.getId(),
                grupo.getNome(),
                convidado.getEmail(),
                idsProjetosSalvos,
                conviteGrupo.getDthRegistro(),
                conviteGrupo.getDthExpiracao(),
                conviteGrupo.isAtivo()
        );
    }

    @Transactional
    public ConviteGrupoResponse gerarConviteGrupo(Usuario remetente, GerarConviteGrupoRequest request) {
        Grupo grupo = grupoService.findById(request.idGrupo())
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + request.idGrupo()));

        validarPermissaoConviteGrupo(remetente, request.idGrupo());

        List<Projeto> projetos = validarProjetos(request.idGrupo(), request.idsProjeto());

        Role role = null;
        if (request.idRole() != null) {
            role = roleRepository.findById(request.idRole())
                    .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado com ID: " + request.idRole()));
        }

        Integer limiteUso = request.limiteUso();
        if (role != null && isLiderRole(role)) {
            limiteUso = 1;
        }

        String token = UUID.randomUUID().toString();
        int diasValidade = (request.diasValidade() != null && request.diasValidade() > 0) ? request.diasValidade() : 7;
        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(diasValidade);

        ConviteGrupo conviteGrupo = new ConviteGrupo(grupo, remetente, token, projetos, dthExpiracao, role, limiteUso);
        conviteGrupo = conviteGrupoRepository.save(conviteGrupo);

        List<Long> idsProjetosSalvos = conviteGrupo.getProjetos() != null
                ? conviteGrupo.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        String urlConvite = "/convites/aceitar/grupo/" + token;
        String nomeCargo = role != null ? role.getNome() : null;

        return new ConviteGrupoResponse(
                conviteGrupo.getId(),
                token,
                urlConvite,
                grupo.getId(),
                grupo.getNome(),
                idsProjetosSalvos,
                conviteGrupo.getDthRegistro(),
                conviteGrupo.getDthExpiracao(),
                conviteGrupo.isAtivo(),
                nomeCargo,
                conviteGrupo.getLimiteUso(),
                conviteGrupo.getUsos()
        );
    }

    @Transactional
    public ConviteCadastroResponse gerarConviteCadastro(Usuario remetente, GerarConviteCadastroRequest request) {

        if(remetente.getCredenciaisLogin().getAccessLevel() != AccessLevel.ADMIN) {
            throw new SemPermissaoConvidarErro("Você não tem permissão para enviar convites de cadastro.");
        }

        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();

        ConviteCadastro convite = new ConviteCadastro(
                null,
                token,
                request.email(),
                now.plusDays(request.diasValidade()),
                now,
                true
        );

        convite = conviteCadastroRepository.save(convite);

        String url = "/convites/aceitar/cadastro/" + token;

        return new ConviteCadastroResponse(
                convite.getId(),
                token,
                url,
                convite.getDthExpiracao()
        );
    }

    @Transactional
    public ConviteGrupoEnviadoResponse aceitarConviteGrupo(String token, Usuario usuarioLogado) {
        ConviteGrupo conviteGrupo = conviteRepository.findByTokenAndIsAtivoTrue(token)
                .orElseThrow(() -> new ConviteInvalidoOuExpiradoErro("Convite por link inválido ou inativo"));

        if (conviteGrupo.getDthExpiracao().isBefore(LocalDateTime.now())) {
            conviteGrupo.setAtivo(false);
            conviteRepository.save(conviteGrupo);
            throw new ConviteInvalidoOuExpiradoErro("Este convite por link já expirou");
        }

        if (conviteGrupo.getLimiteUso() != null && conviteGrupo.getUsos() != null && conviteGrupo.getUsos() >= conviteGrupo.getLimiteUso()) {
            conviteGrupo.setAtivo(false);
            conviteRepository.save(conviteGrupo);
            throw new ConviteInvalidoOuExpiradoErro("Este convite por link já atingiu o limite de usos");
        }

        Grupo grupo = conviteGrupo.getGrupo();
        if (grupoXUsuarioService.isUsuarioNoGrupo(grupo.getId(), usuarioLogado.getId())) {
            throw new UsuarioJaNoGrupoErro("Você já pertence a este grupo");
        }

        Role role = conviteGrupo.getRole();
        if (role == null) {
            role = roleRepository.findByNome("Membro")
                    .orElseGet(() -> roleRepository.findAll().stream().findFirst().orElse(null));
        }

        GrupoXUsuario gxu = new GrupoXUsuario(grupo, usuarioLogado, role, true);
        grupoXUsuarioRepository.save(gxu);

        int novosUsos = (conviteGrupo.getUsos() == null ? 0 : conviteGrupo.getUsos()) + 1;
        conviteGrupo.setUsos(novosUsos);
        if (conviteGrupo.getLimiteUso() != null && novosUsos >= conviteGrupo.getLimiteUso()) {
            conviteGrupo.setAtivo(false);
        }
        conviteRepository.save(conviteGrupo);

        List<Long> idsProjetos = conviteGrupo.getProjetos() != null
                ? conviteGrupo.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        return new ConviteGrupoEnviadoResponse(
                conviteGrupo.getId(),
                grupo.getId(),
                grupo.getNome(),
                usuarioLogado.getEmail(),
                idsProjetos,
                conviteGrupo.getDthRegistro(),
                conviteGrupo.getDthExpiracao(),
                conviteGrupo.isAtivo()
        );
    }

    private List<Projeto> validarProjetos(Long grupoId, List<Long> idsProjeto) {
        if (idsProjeto == null || idsProjeto.isEmpty()) {
            return List.of();
        }
        List<Projeto> projetos = projetoService.findAllByIds(idsProjeto);
        if (projetos.size() != idsProjeto.size()) {
            throw new ProjetoNaoEncontradoErro("Um ou mais projetos informados não foram encontrados");
        }
        for (Projeto projeto : projetos) {
            if (!projeto.getGrupo().getId().equals(grupoId)) {
                throw new ProjetoNaoPertenceAoGrupoErro("O projeto '" + projeto.getTitulo() + "' não pertence ao grupo informado");
            }
        }
        return projetos;
    }

    private void validarPermissaoConviteGrupo(Usuario remetente, Long grupoId) {
        boolean isAdmin = remetente.getCredenciaisLogin() != null &&
                remetente.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;

        if (isAdmin) {
            return;
        }

        Optional<GrupoXUsuario> gxuOpt = grupoXUsuarioService.findByGrupoIdAndUsuarioId(grupoId, remetente.getId());
        if (gxuOpt.isEmpty()) {
            throw new SemPermissaoConvidarErro("Você não possui permissão para enviar convites para este grupo");
        }

        GrupoXUsuario gxu = gxuOpt.get();
        if (!isLiderRole(gxu.getRole())) {
            throw new SemPermissaoConvidarErro("Você não possui permissão para enviar convites para este grupo. É necessário ser líder do grupo.");
        }
    }

    private boolean isLiderRole(Role role) {
        if (role == null) {
            return false;
        }
        return role.isLider();
    }
}
