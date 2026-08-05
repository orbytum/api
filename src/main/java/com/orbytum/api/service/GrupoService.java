package com.orbytum.api.service;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoConvidarErro;
import com.orbytum.api.repository.GrupoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoXUsuarioService grupoXUsuarioService;

    public GrupoService(GrupoRepository grupoRepository, GrupoXUsuarioService grupoXUsuarioService) {
        this.grupoRepository = grupoRepository;
        this.grupoXUsuarioService = grupoXUsuarioService;
    }

    public Optional<Grupo> findById(Long id) {
        return grupoRepository.findById(id);
    }

    public Grupo save(Grupo grupo) {
        return grupoRepository.save(grupo);
    }

}
