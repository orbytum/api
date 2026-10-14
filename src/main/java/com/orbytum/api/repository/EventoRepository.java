package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Evento;
import com.orbytum.api.models.enums.TipoLembrete;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
              AND (:tipo IS NULL OR e.tipo = :tipo)
              AND (:busca IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.organizador) LIKE LOWER(CONCAT('%', :busca, '%')))
            ORDER BY e.dthInicio ASC
            """,
            countQuery = """
            SELECT COUNT(e) FROM Evento e
            WHERE e.grupo.id = :grupoId
              AND e.isAtivo = true
              AND (:tipo IS NULL OR e.tipo = :tipo)
              AND (:busca IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                                 OR LOWER(e.organizador) LIKE LOWER(CONCAT('%', :busca, '%')))
            """)
    Page<Evento> filtrarPorGrupo(
            @Param("grupoId") Long grupoId,
            @Param("tipo") TipoLembrete tipo,
            @Param("busca") String busca,
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
