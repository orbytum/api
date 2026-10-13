package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Noticia;
import com.orbytum.api.models.enums.NoticiaStatus;
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
public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    Optional<Noticia> findByIdAndIsAtivoTrue(Long id);

    Optional<Noticia> findByUuidAndIsAtivoTrue(UUID uuid);

    Page<Noticia> findByGrupoIdAndStatusAndIsAtivoTrue(
            Long grupoId, NoticiaStatus status, Pageable pageable);

    long countByGrupoIdAndIsAtivoTrue(Long grupoId);

    @Query(value = """
            SELECT n FROM Noticia n
            LEFT JOIN FETCH n.grupo g
            LEFT JOIN FETCH n.autor a
            WHERE n.isAtivo = true
              AND n.status = :status
              AND g.id = :grupoId
              AND (n.dataInicioExibicao IS NULL OR n.dataInicioExibicao <= :agora)
              AND n.dataFimExibicao IS NOT NULL
              AND n.dataFimExibicao >= :agora
              AND (CAST(:assunto AS string) IS NULL OR CAST(:assunto AS string) = '' OR LOWER(n.assunto) LIKE LOWER(CONCAT('%', CAST(:assunto AS string), '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR n.dthRegistro >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR n.dthRegistro <= :dataFim)
            """,
            countQuery = """
            SELECT COUNT(n) FROM Noticia n
            WHERE n.isAtivo = true
              AND n.status = :status
              AND n.grupo.id = :grupoId
              AND (n.dataInicioExibicao IS NULL OR n.dataInicioExibicao <= :agora)
              AND n.dataFimExibicao IS NOT NULL
              AND n.dataFimExibicao >= :agora
              AND (CAST(:assunto AS string) IS NULL OR CAST(:assunto AS string) = '' OR LOWER(n.assunto) LIKE LOWER(CONCAT('%', CAST(:assunto AS string), '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR n.dthRegistro >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR n.dthRegistro <= :dataFim)
            """)
    Page<Noticia> filtrarVigentes(
            @Param("grupoId") Long grupoId,
            @Param("status") NoticiaStatus status,
            @Param("agora") LocalDateTime agora,
            @Param("assunto") String assunto,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable);
}
