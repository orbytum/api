package com.orbytum.api.repository;

import com.orbytum.api.models.entity.MaterialEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialEmprestimoRepository extends JpaRepository<MaterialEmprestimo, Long> {

    Optional<MaterialEmprestimo> findFirstByMaterialIdAndIsDevolvidoFalseOrderByDthInicioDesc(Long materialId);
}
