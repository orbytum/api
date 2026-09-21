package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByIdAndIsAtivoTrue(Long id);
}
