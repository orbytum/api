package com.orbytum.api.service;

import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GrupoXUsuarioService {

    private final GrupoXUsuarioRepository grupoXUsuarioRepository;

    public GrupoXUsuarioService(GrupoXUsuarioRepository grupoXUsuarioRepository) {
        this.grupoXUsuarioRepository = grupoXUsuarioRepository;
    }

    public List<Permissao> findPermissoesByUsuario(Usuario usuario) {
        return grupoXUsuarioRepository.findPermissoesByUsuario(usuario);
    }

    public Optional<GrupoXUsuario> findByGrupoIdAndUsuarioId(Long grupoId, Long usuarioId) {
        return grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuarioId);
    }

    public boolean isUsuarioNoGrupo(Long grupoId, Long usuarioId) {
        return grupoXUsuarioRepository.existsByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuarioId);
    }

    public boolean isUsuarioNoGrupo(Long grupoId, String email) {
        return grupoXUsuarioRepository.existsByGrupoIdAndUsuarioEmailAndIsAtivoTrue(grupoId, email);
    }
}
