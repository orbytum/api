package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Publicacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicacaoRepository extends JpaRepository<Publicacao, Long> {

    Optional<Publicacao> findByIdAndIsAtivoTrue(Long id);

    Optional<Publicacao> findByUuidAndIsAtivoTrue(UUID uuid);

    @Query(value = """
            SELECT p FROM Publicacao p
            LEFT JOIN FETCH p.projeto proj
            WHERE p.isAtivo = true
              AND (CAST(:titulo AS string) IS NULL OR CAST(:titulo AS string) = '' OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:titulo AS string), '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR p.dthRegistro >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR p.dthRegistro <= :dataFim)
              AND (:projetoId IS NULL OR proj.id = :projetoId)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Publicacao p
            WHERE p.isAtivo = true
              AND (CAST(:titulo AS string) IS NULL OR CAST(:titulo AS string) = '' OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:titulo AS string), '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR p.dthRegistro >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR p.dthRegistro <= :dataFim)
              AND (:projetoId IS NULL OR p.projeto.id = :projetoId)
            """)
    Page<Publicacao> filtrarPublicacoes(
            @Param("titulo") String titulo,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("projetoId") Long projetoId,
            Pageable pageable
    );
}
