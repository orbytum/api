package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Lembrete;
import com.orbytum.api.models.enums.TipoLembrete;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LembreteRepository extends JpaRepository<Lembrete, Long> {

    Optional<Lembrete> findByIdAndIsAtivoTrue(Long id);

    @Query(value = """
            SELECT DISTINCT l FROM Lembrete l
            JOIN l.grupo g
            LEFT JOIN FETCH l.organizador o
            LEFT JOIN FETCH l.participantes p
            WHERE l.isAtivo = true
              AND (:grupoId IS NULL OR g.id = :grupoId)
              AND (:tipo IS NULL OR l.tipo = :tipo)
              AND (CAST(:busca AS string) IS NULL OR CAST(:busca AS string) = '' OR 
                   LOWER(l.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')) OR 
                   LOWER(l.descricao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')) OR
                   LOWER(o.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            ORDER BY l.dataHora DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT l) FROM Lembrete l
            WHERE l.isAtivo = true
              AND (:grupoId IS NULL OR l.grupo.id = :grupoId)
              AND (:tipo IS NULL OR l.tipo = :tipo)
              AND (CAST(:busca AS string) IS NULL OR CAST(:busca AS string) = '' OR 
                   LOWER(l.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')) OR 
                   LOWER(l.descricao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')) OR
                   LOWER(l.organizador.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    Page<Lembrete> filtrarLembretes(
            @Param("grupoId") Long grupoId,
            @Param("tipo") TipoLembrete tipo,
            @Param("busca") String busca,
            Pageable pageable
    );
}
