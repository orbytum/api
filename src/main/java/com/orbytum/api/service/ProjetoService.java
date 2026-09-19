package com.orbytum.api.service;

import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.enums.ProjetoStatus;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.ProjetoNaoPodeSerFinalizado;
import com.orbytum.api.repository.ProjetoRepository;
import jakarta.transaction.Transactional;
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

    @Transactional
    public Projeto save(Projeto projeto) {
        return projetoRepository.save(projeto);
    }

    public List<Projeto> findAllAtivos() {
        return projetoRepository.findAllByIsAtivoTrue();
    }

    @Transactional
    public void delete(Projeto projeto) {
        var grupoProjeto = projeto.getGrupo();

        //REQ-025
        if (projeto.isInicial())
        {
            var projetosNaoFinalizados = grupoProjeto.getProjetos().stream().filter(p -> !(p.getStatus() == ProjetoStatus.ENCERRADO || p.getStatus() == ProjetoStatus.CANCELADO)).toList();
            if (projetosNaoFinalizados.size() > 1) throw new ProjetoNaoPodeSerFinalizado("O projeto Inicial só pode ser finalizado se não houver outros projetos ativos");
        }

        projeto.setAtivo(false);
        projetoRepository.save(projeto);
    }

    public List<Projeto> findAllAtivosByGrupoId(Long grupoId) {
        return projetoRepository.findAllByGrupoIdAndIsAtivoTrue(grupoId);
    }
}
