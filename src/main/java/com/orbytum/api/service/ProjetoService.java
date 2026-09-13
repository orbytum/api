package com.orbytum.api.service;

import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.repository.ProjetoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;

    public ProjetoService(ProjetoRepository projetoRepository) {
        this.projetoRepository = projetoRepository;
    }

    public Optional<Projeto> findById(Long id) {
        return projetoRepository.findById(id);
    }

    public List<Projeto> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return projetoRepository.findAllByIdIn(ids);
    }
}
