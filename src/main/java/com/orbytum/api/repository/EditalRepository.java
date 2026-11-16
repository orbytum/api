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
              AND (:projetoId IS NULL OR EXISTS (SELECT 1 FROM e.projetos p WHERE p.id = :projetoId))
              AND (:somenteRelevantes = false OR EXISTS (
                    SELECT 1 FROM Grupo gk JOIN gk.palavrasChave k
                    WHERE gk.id = :grupoId
                      AND (LOWER(e.descricao) LIKE CONCAT('%', LOWER(k), '%')
                           OR LOWER(e.organizador) LIKE CONCAT('%', LOWER(k), '%'))))
              AND (:gestor = true
                   OR NOT EXISTS (SELECT 1 FROM e.projetos pv WHERE pv.grupo.id = :grupoId)
                   OR EXISTS (SELECT 1 FROM e.projetos pm WHERE pm.grupo.id = :grupoId AND pm.id IN (
                        SELECT pxu.projeto.id FROM ProjetoXUsuario pxu
                        WHERE pxu.usuario.id = :usuarioId AND pxu.isAtivo = true)))
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
              AND (:projetoId IS NULL OR EXISTS (SELECT 1 FROM e.projetos p WHERE p.id = :projetoId))
              AND (:somenteRelevantes = false OR EXISTS (
                    SELECT 1 FROM Grupo gk JOIN gk.palavrasChave k
                    WHERE gk.id = :grupoId
                      AND (LOWER(e.descricao) LIKE CONCAT('%', LOWER(k), '%')
                           OR LOWER(e.organizador) LIKE CONCAT('%', LOWER(k), '%'))))
              AND (:gestor = true
                   OR NOT EXISTS (SELECT 1 FROM e.projetos pv WHERE pv.grupo.id = :grupoId)
                   OR EXISTS (SELECT 1 FROM e.projetos pm WHERE pm.grupo.id = :grupoId AND pm.id IN (
                        SELECT pxu.projeto.id FROM ProjetoXUsuario pxu
                        WHERE pxu.usuario.id = :usuarioId AND pxu.isAtivo = true)))
            """)
    Page<Edital> filtrarPorGrupo(
            @Param("grupoId") Long grupoId,
            @Param("projetoId") Long projetoId,
            @Param("palavraChave") String palavraChave,
            @Param("organizacao") String organizacao,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("somenteRelevantes") boolean somenteRelevantes,
            @Param("gestor") boolean gestor,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);
}
