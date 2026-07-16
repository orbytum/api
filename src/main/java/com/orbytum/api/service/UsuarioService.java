package com.orbytum.api.service;

import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.repository.UsuarioRepository;

import java.util.Optional;

public class UsuarioService {

    private UsuarioRepository usuarioRepository;
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}
