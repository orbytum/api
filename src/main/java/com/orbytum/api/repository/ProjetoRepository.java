package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    List<Projeto> findAllByIdIn(List<Long> ids);
}
