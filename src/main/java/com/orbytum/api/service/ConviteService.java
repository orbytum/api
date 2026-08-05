package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.GerarLinkConviteRequest;
import com.orbytum.api.models.dto.response.ConviteEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteLinkResponse;
import com.orbytum.api.models.entity.Convite;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.models.exceptions.*;
import com.orbytum.api.repository.ConviteRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConviteService {

    private final ConviteRepository conviteRepository;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioService grupoXUsuarioService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final ProjetoService projetoService;
    private final RoleRepository roleRepository;

    public ConviteService(ConviteRepository conviteRepository,
                          GrupoService grupoService,
                          UsuarioService usuarioService,
                          GrupoXUsuarioService grupoXUsuarioService,
                          GrupoXUsuarioRepository grupoXUsuarioRepository,
                          ProjetoService projetoService,
                          RoleRepository roleRepository) {
        this.conviteRepository = conviteRepository;
        this.grupoService = grupoService;
        this.usuarioService = usuarioService;
        this.grupoXUsuarioService = grupoXUsuarioService;
        this.grupoXUsuarioRepository = grupoXUsuarioRepository;
        this.projetoService = projetoService;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public ConviteEnviadoResponse enviarConvite(Usuario remetente, Long grupoId, String emailConvidado, List<Long> projetoIds) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + grupoId));

        validarPermissaoConvite(remetente, grupoId);

        Usuario convidado = usuarioService.findByEmail(emailConvidado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário convidado não encontrado com e-mail: " + emailConvidado));

        if (grupoXUsuarioService.isUsuarioNoGrupo(grupoId, convidado.getId())) {
            throw new UsuarioJaNoGrupoErro("O usuário convidado já pertence a este grupo");
        }

        List<Projeto> projetos = validarProjetos(grupoId, projetoIds);

        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(7);
        Convite convite = new Convite(grupo, convidado, remetente, projetos, dthExpiracao);
        convite = conviteRepository.save(convite);

        List<Long> idsProjetosSalvos = convite.getProjetos() != null
                ? convite.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        return new ConviteEnviadoResponse(
                convite.getId(),
                grupo.getId(),
                grupo.getNome(),
                convidado.getEmail(),
                idsProjetosSalvos,
                convite.getDthRegistro(),
                convite.getDthExpiracao(),
                convite.isAtivo()
        );
    }

    @Transactional
    public ConviteLinkResponse gerarLinkConvite(Usuario remetente, GerarLinkConviteRequest request) {
        Grupo grupo = grupoService.findById(request.idGrupo())
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + request.idGrupo()));

        validarPermissaoConvite(remetente, request.idGrupo());

        List<Projeto> projetos = validarProjetos(request.idGrupo(), request.idsProjeto());

        String token = UUID.randomUUID().toString();
        int diasValidade = (request.diasValidade() != null && request.diasValidade() > 0) ? request.diasValidade() : 7;
        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(diasValidade);

        Convite convite = new Convite(grupo, remetente, token, projetos, dthExpiracao);
        convite = conviteRepository.save(convite);

        List<Long> idsProjetosSalvos = convite.getProjetos() != null
                ? convite.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        String urlConvite = "/convites/aceitar-link/" + token;

        return new ConviteLinkResponse(
                convite.getId(),
                token,
                urlConvite,
                grupo.getId(),
                grupo.getNome(),
                idsProjetosSalvos,
                convite.getDthRegistro(),
                convite.getDthExpiracao(),
                convite.isAtivo()
        );
    }

    @Transactional
    public ConviteEnviadoResponse aceitarConvitePorLink(String token, Usuario usuarioLogado) {
        Convite convite = conviteRepository.findByTokenAndIsAtivoTrue(token)
                .orElseThrow(() -> new ConviteInvalidoOuExpiradoErro("Convite por link inválido ou inativo"));

        if (convite.getDthExpiracao().isBefore(LocalDateTime.now())) {
            convite.setAtivo(false);
            conviteRepository.save(convite);
            throw new ConviteInvalidoOuExpiradoErro("Este convite por link já expirou");
        }

        Grupo grupo = convite.getGrupo();
        if (grupoXUsuarioService.isUsuarioNoGrupo(grupo.getId(), usuarioLogado.getId())) {
            throw new UsuarioJaNoGrupoErro("Você já pertence a este grupo");
        }

        Role rolePadrao = roleRepository.findByNome("Membro")
                .orElseGet(() -> roleRepository.findAll().stream().findFirst().orElse(null));

        GrupoXUsuario gxu = new GrupoXUsuario(grupo, usuarioLogado, rolePadrao, true);
        grupoXUsuarioRepository.save(gxu);

        List<Long> idsProjetos = convite.getProjetos() != null
                ? convite.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        return new ConviteEnviadoResponse(
                convite.getId(),
                grupo.getId(),
                grupo.getNome(),
                usuarioLogado.getEmail(),
                idsProjetos,
                convite.getDthRegistro(),
                convite.getDthExpiracao(),
                convite.isAtivo()
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

    private void validarPermissaoConvite(Usuario remetente, Long grupoId) {
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
        boolean temPermissao = gxu.getRole() != null &&
                gxu.getRole().getPermissoes() != null &&
                gxu.getRole().getPermissoes().contains(Permissao.PROJETO_CONVITE_CRIAR);

        if (!temPermissao) {
            throw new SemPermissaoConvidarErro("Você não possui permissão para enviar convites para este grupo");
        }
    }
}
