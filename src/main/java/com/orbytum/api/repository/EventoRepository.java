package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Evento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    Optional<Evento> findByIdAndIsAtivoTrue(Long id);

    @Query("""
            SELECT e FROM Evento e
            LEFT JOIN FETCH e.grupo
            LEFT JOIN FETCH e.criador
            LEFT JOIN FETCH e.participantes
            WHERE e.id = :id AND e.isAtivo = true
            """)
    Optional<Evento> findByIdComRelacionamentos(@Param("id") Long id);

    @Query(value = """
            SELECT DISTINCT e FROM Evento e
            LEFT JOIN FETCH e.grupo
            LEFT JOIN FETCH e.criador
            LEFT JOIN FETCH e.participantes
            WHERE e.grupo.id = :grupoId
              AND e.isAtivo = true
              AND (:busca = '' OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.organizador) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR e.dthInicio >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR e.dthInicio <= :dataFim)
            ORDER BY e.dthInicio ASC
            """,
            countQuery = """
            SELECT COUNT(e) FROM Evento e
            WHERE e.grupo.id = :grupoId
              AND e.isAtivo = true
              AND (:busca = '' OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.organizador) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (CAST(:dataInicio AS java.time.LocalDateTime) IS NULL OR e.dthInicio >= :dataInicio)
              AND (CAST(:dataFim AS java.time.LocalDateTime) IS NULL OR e.dthInicio <= :dataFim)
            """)
    /** `busca` nunca deve ser nulo (use "" para sem filtro): parâmetro String nulo vira bytea no PostgreSQL. */
    Page<Evento> filtrarPorGrupo(
            @Param("grupoId") Long grupoId,
            @Param("busca") String busca,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Evento e
            LEFT JOIN FETCH e.grupo
            LEFT JOIN FETCH e.criador
            LEFT JOIN FETCH e.participantes
            WHERE e.grupo.id = :grupoId
              AND e.isAtivo = true
            ORDER BY e.dthInicio ASC
            """)
    List<Evento> findAllByGrupoIdAndIsAtivoTrueOrderByDthInicioAsc(@Param("grupoId") Long grupoId);
}
