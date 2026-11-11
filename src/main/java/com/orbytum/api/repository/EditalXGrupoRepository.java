package com.orbytum.api.repository;

import com.orbytum.api.models.entity.joinColumns.EditalXGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EditalXGrupoRepository extends JpaRepository<EditalXGrupo, Long> {

    boolean existsByEditalIdAndGrupoId(Long editalId, Long grupoId);

    Optional<EditalXGrupo> findByEditalIdAndGrupoId(Long editalId, Long grupoId);

    long countByEditalId(Long editalId);

    List<EditalXGrupo> findAllByGrupoId(Long grupoId);
}
