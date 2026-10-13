package com.orbytum.api.repository;

import com.orbytum.api.models.entity.MaterialEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialEmprestimoRepository extends JpaRepository<MaterialEmprestimo, Long> {

    Optional<MaterialEmprestimo> findFirstByMaterialIdAndIsDevolvidoFalseOrderByDthInicioDesc(Long materialId);

    @Query("SELECT COALESCE(SUM(e.quantidade), 0) FROM MaterialEmprestimo e " +
            "WHERE e.material.id = :materialId AND e.isDevolvido = false")
    Integer somarQuantidadeEmprestada(@Param("materialId") Long materialId);
}
