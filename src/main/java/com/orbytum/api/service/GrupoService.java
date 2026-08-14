package com.orbytum.api.service;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.repository.GrupoRepository;
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
