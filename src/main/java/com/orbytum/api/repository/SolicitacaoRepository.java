package com.orbytum.api.repository;

import com.orbytum.api.models.entity.MaterialEmprestimoSolicitacao;
import com.orbytum.api.models.enums.SolicitacaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitacaoRepository extends JpaRepository<MaterialEmprestimoSolicitacao, Long> {

    List<MaterialEmprestimoSolicitacao> findAllByProjetoId(Long projetoId);

    List<MaterialEmprestimoSolicitacao> findAllByProjetoGrupoId(Long grupoId);

    @Query("SELECT s FROM MaterialEmprestimoSolicitacao s JOIN s.items i " +
            "WHERE i.material.id = :materialId AND s.status = :status")
    List<MaterialEmprestimoSolicitacao> findByMaterialIdAndStatus(
            @Param("materialId") Long materialId,
            @Param("status") SolicitacaoStatus status);
}
