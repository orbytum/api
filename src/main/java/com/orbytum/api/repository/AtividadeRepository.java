package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.enums.AtividadeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    @Query("""
            SELECT a FROM Atividade a
            LEFT JOIN FETCH a.projeto
            LEFT JOIN FETCH a.responsavel
            WHERE a.id = :id
            """)
    Optional<Atividade> findByIdComRelacionamentos(@Param("id") Long id);

    @Query(value = """
            SELECT a FROM Atividade a
            LEFT JOIN FETCH a.projeto
            LEFT JOIN FETCH a.responsavel
            WHERE a.projeto.id = :projetoId
              AND a.isAtivo = true
            """,
            countQuery = """
            SELECT COUNT(a) FROM Atividade a
            WHERE a.projeto.id = :projetoId
              AND a.isAtivo = true
            """)
    Page<Atividade> pageAtivasPorProjeto(@Param("projetoId") Long projetoId, Pageable pageable);

    @Query("""
            SELECT a FROM Atividade a
            LEFT JOIN FETCH a.projeto
            LEFT JOIN FETCH a.responsavel
            WHERE a.projeto.id = :projetoId
              AND a.isAtivo = true
              AND a.status IN :status
              AND a.dthPrazo IS NOT NULL
              AND a.dthPrazo < :agora
            ORDER BY a.dthPrazo ASC
            """)
    List<Atividade> findAllAtrasadasPorProjeto(
            @Param("projetoId") Long projetoId,
            @Param("status") Collection<AtividadeStatus> status,
            @Param("agora") LocalDateTime agora);

    List<Atividade> findAllByAtividadePaiIdAndIsAtivoTrue(Long atividadePaiId);

    long countByProjetoIdAndStatusInAndIsAtivoTrue(Long projetoId, Collection<AtividadeStatus> status);
}
