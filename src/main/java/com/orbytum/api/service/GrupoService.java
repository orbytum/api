package com.orbytum.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.repository.GrupoRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoXUsuarioService grupoXUsuarioService;


    public boolean existsByNome(String nome) {
        return grupoRepository.existsByNome(nome);
    }

    @Transactional
    public Grupo save(Grupo grupo) {
        return grupoRepository.save(grupo);
    }

    public Optional<Grupo> findById(Long id) {
        return grupoRepository.findById(id);
    }

    public List<Grupo> findAllAtivos() {
        return grupoRepository.findAllByIsAtivoTrue();
    }

    public List<Grupo> findAllByCriador(Usuario criador) {
        return grupoRepository.findAllByCriadorAndIsAtivoTrue(criador);
    }
}
