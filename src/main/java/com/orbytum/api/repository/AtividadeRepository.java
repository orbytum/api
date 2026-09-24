package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.enums.AtividadeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    List<Atividade> findAllByProjetoIdAndIsAtivoTrue(Long projetoId);

    List<Atividade> findAllByProjetoIdAndStatusInAndIsAtivoTrue(Long projetoId, Collection<AtividadeStatus> status);

    List<Atividade> findAllByAtividadePaiIdAndIsAtivoTrue(Long atividadePaiId);

    long countByProjetoIdAndStatusInAndIsAtivoTrue(Long projetoId, Collection<AtividadeStatus> status);
}
