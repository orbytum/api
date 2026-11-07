package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Edital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EditalRepository extends JpaRepository<Edital, Long> {

    Optional<Edital> findByUri(String uri);

    @Query(value = """
            SELECT DISTINCT e FROM Edital e
            JOIN e.grupos eg
            WHERE eg.grupo.id = :grupoId
              AND (CAST(:palavraChave AS string) IS NULL OR CAST(:palavraChave AS string) = ''
                   OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', CAST(:palavraChave AS string), '%')))
              AND (CAST(:organizacao AS string) IS NULL OR CAST(:organizacao AS string) = ''
                   OR LOWER(e.organizador) LIKE LOWER(CONCAT('%', CAST(:organizacao AS string), '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR e.dthAbertura >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR e.dthEncerramento <= :dataFim)
            ORDER BY e.dthEncerramento ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e) FROM Edital e
            JOIN e.grupos eg
            WHERE eg.grupo.id = :grupoId
              AND (CAST(:palavraChave AS string) IS NULL OR CAST(:palavraChave AS string) = ''
                   OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', CAST(:palavraChave AS string), '%')))
              AND (CAST(:organizacao AS string) IS NULL OR CAST(:organizacao AS string) = ''
                   OR LOWER(e.organizador) LIKE LOWER(CONCAT('%', CAST(:organizacao AS string), '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR e.dthAbertura >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR e.dthEncerramento <= :dataFim)
            """)
    Page<Edital> filtrarPorGrupo(
            @Param("grupoId") Long grupoId,
            @Param("palavraChave") String palavraChave,
            @Param("organizacao") String organizacao,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable);
}
