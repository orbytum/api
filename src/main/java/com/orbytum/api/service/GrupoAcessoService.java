package com.orbytum.api.service;

import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrupoAcessoService {

    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final ProjetoXUsuarioRepository projetoXUsuarioRepository;

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
