package com.orbytum.api.service;

import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrupoXUsuarioService {

    private final GrupoXUsuarioRepository grupoXUsuarioRepository;

    public GrupoXUsuarioService(GrupoXUsuarioRepository grupoXUsuarioRepository) {
        this.grupoXUsuarioRepository = grupoXUsuarioRepository;
    }

    public List<Permissao> findPermissoesByUsuario(Usuario usuario) {
        return grupoXUsuarioRepository.findPermissoesByUsuario(usuario);
    }
}
