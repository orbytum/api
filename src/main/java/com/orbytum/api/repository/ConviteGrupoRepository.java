package com.orbytum.api.repository;

import com.orbytum.api.models.entity.ConviteGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConviteGrupoRepository extends JpaRepository<ConviteGrupo, Long> {
    Optional<ConviteGrupo> findByTokenAndIsAtivoTrue(String token);
}
