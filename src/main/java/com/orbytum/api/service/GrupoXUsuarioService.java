package com.orbytum.api.service;

import com.orbytum.api.model.entity.Usuario;
import com.orbytum.api.model.enums.Permissao;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GrupoXUsuarioService {

    public List<Permissao> findPermissoesByUsuario(Usuario usuario) {
        return Collections.emptyList();
    }
}
