package com.orbytum.api.service;

import com.orbytum.api.models.dto.response.ConviteEnviadoResponse;
import com.orbytum.api.models.entity.Convite;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoConvidarErro;
import com.orbytum.api.models.exceptions.UsuarioJaNoGrupoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.ConviteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ConviteService {

    private final ConviteRepository conviteRepository;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioService grupoXUsuarioService;

    public ConviteService(ConviteRepository conviteRepository,
                          GrupoService grupoService,
                          UsuarioService usuarioService,
                          GrupoXUsuarioService grupoXUsuarioService) {
        this.conviteRepository = conviteRepository;
        this.grupoService = grupoService;
        this.usuarioService = usuarioService;
        this.grupoXUsuarioService = grupoXUsuarioService;
    }

    @Transactional
    public ConviteEnviadoResponse enviarConvite(Usuario remetente, Long grupoId, String emailConvidado) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + grupoId));

        validarPermissaoConvidar(remetente, grupoId);

        Usuario convidado = usuarioService.findByEmail(emailConvidado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário convidado não encontrado com e-mail: " + emailConvidado));

        if (grupoXUsuarioService.isUsuarioNoGrupo(grupoId, convidado.getId())) {
            throw new UsuarioJaNoGrupoErro("O usuário convidado já pertence a este grupo");
        }

        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(7);
        Convite convite = new Convite(grupo, convidado, remetente, dthExpiracao);
        convite = conviteRepository.save(convite);

        return new ConviteEnviadoResponse(
                convite.getId(),
                grupo.getId(),
                grupo.getNome(),
                convidado.getEmail(),
                convite.getDthRegistro(),
                convite.getDthExpiracao(),
                convite.isAtivo()
        );
    }

    private void validarPermissaoConvidar(Usuario remetente, Long grupoId) {
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
