package com.orbytum.api.service;

import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrupoAcessoService {

    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final ProjetoXUsuarioRepository projetoXUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public Optional<GrupoXUsuario> vinculo(Long grupoId, Long usuarioId) {
        if (grupoId == null || usuarioId == null) {
            return Optional.empty();
        }
        return grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuarioId);
    }

    public boolean isMembro(Long grupoId, Long usuarioId) {
        return vinculo(grupoId, usuarioId).isPresent();
    }

    public NivelMembro nivelDoUsuario(Long grupoId, Long usuarioId) {
        return vinculo(grupoId, usuarioId)
                .map(GrupoXUsuario::getNivel)
                .orElse(null);
    }

    public boolean isLider(Long grupoId, Long usuarioId) {
        return isMembro(grupoId, usuarioId) && nivelDoUsuario(grupoId, usuarioId) == NivelMembro.LIDER;
    }

    public boolean isCoordenadorOuAcima(Long grupoId, Long usuarioId) {
        NivelMembro nivel = nivelDoUsuario(grupoId, usuarioId);
        return nivel != null && nivel.isPeloMenos(NivelMembro.COORDENADOR);
    }

    public boolean isColaborador(Long grupoId, Long usuarioId) {
        return nivelDoUsuario(grupoId, usuarioId) == NivelMembro.COLABORADOR;
    }

    public boolean isAdmin(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return usuarioRepository.findById(usuarioId)
                .map(usuario -> usuario.getCredenciaisLogin() != null
                        && (usuario.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN
                        || usuario.getCredenciaisLogin().getAccessLevel() == AccessLevel.INITIAL_ADMIN))
                .orElse(false);
    }

    public boolean isColaboradorDoProjeto(Long projetoId, Long usuarioId) {
        if (projetoId == null || usuarioId == null) {
            return false;
        }
        return projetoXUsuarioRepository
                .findByProjetoIdAndUsuarioIdAndIsAtivoTrue(projetoId, usuarioId)
                .map(ProjetoXUsuario::getNivel)
                .filter(nivel -> nivel == NivelMembro.COLABORADOR)
                .isPresent();
    }

    public boolean isLiderDoProjeto(Long projetoId, Long usuarioId) {
        if (projetoId == null || usuarioId == null) {
            return false;
        }
        return projetoXUsuarioRepository
                .findByProjetoIdAndUsuarioIdAndIsAtivoTrue(projetoId, usuarioId)
                .map(ProjetoXUsuario::getNivel)
                .filter(nivel -> nivel == NivelMembro.LIDER)
                .isPresent();
    }

    /**
     * Regra central de gestão de projeto:
     * admin (tratado pelo chamador) / líder ou coordenador do grupo / líder do projeto.
     * Colaborador no grupo nunca gerencia.
     */
    public boolean podeGerenciarProjeto(Long projetoId, Long usuarioId) {
        if (projetoId == null || usuarioId == null) {
            return false;
        }

        if (isAdmin(usuarioId)) {
            return true;
        }

        Long grupoId = projetoXUsuarioRepository.findGrupoIdByProjetoId(projetoId).orElse(null);
        if (grupoId == null) {
            return false;
        }

        NivelMembro nivelGrupo = nivelDoUsuario(grupoId, usuarioId);
        if (nivelGrupo == NivelMembro.COLABORADOR) {
            return false;
        }
        if (nivelGrupo != null && nivelGrupo.isPeloMenos(NivelMembro.COORDENADOR)) {
            return true;
        }
        return isLiderDoProjeto(projetoId, usuarioId);
    }

    public void validarMembro(Long grupoId, Long usuarioId) {
        if (!isMembro(grupoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("Você não é membro deste grupo de pesquisa");
        }
    }

    public void validarLider(Long grupoId, Long usuarioId) {
        if (!isLider(grupoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("Esta operação exige permissão de Líder do grupo");
        }
    }

    public void validarCoordenadorOuAcima(Long grupoId, Long usuarioId) {
        if (!isCoordenadorOuAcima(grupoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("Esta operação exige permissão de Líder ou Coordenador do grupo");
        }
    }

    public void validarNaoColaborador(Long grupoId, Long usuarioId) {
        if (isColaborador(grupoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("Colaboradores possuem acesso somente leitura a este grupo");
        }
    }

    public void validarNaoColaboradorDoProjeto(Long projetoId, Long usuarioId) {
        if (isColaboradorDoProjeto(projetoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("Colaboradores possuem acesso somente leitura a este projeto");
        }
    }

    public void validarPodeGerenciarProjeto(Long projetoId, Long usuarioId) {
        if (!podeGerenciarProjeto(projetoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("Esta operação exige permissão de Líder do grupo ou do projeto");
        }
    }

    public boolean isMembroDoProjeto(Long projetoId, Long usuarioId) {
        return projetoId != null && usuarioId != null
                && projetoXUsuarioRepository.existsByProjetoIdAndUsuarioIdAndIsAtivoTrue(projetoId, usuarioId);
    }

    public void validarMembroDoProjeto(Long projetoId, Long usuarioId) {
        if (!isMembroDoProjeto(projetoId, usuarioId)) {
            throw new SemPermissaoNoGrupoErro("O usuário informado não participa deste projeto");
        }
    }
}
