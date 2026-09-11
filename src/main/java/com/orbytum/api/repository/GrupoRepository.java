package com.orbytum.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    
    boolean existsByNome(String nome);

    Optional<Grupo> findByNome(String nome);

    List<Grupo> findAllByIsAtivoTrue();

    List<Grupo> findAllByCriadorAndIsAtivoTrue(Usuario criador);

    @Query(value = """
            SELECT DISTINCT g FROM Grupo g
            LEFT JOIN GrupoXUsuario gxu ON gxu.grupo = g AND gxu.isAtivo = true
            LEFT JOIN gxu.usuario u
            LEFT JOIN g.criador c
            WHERE g.isAtivo = true
              AND g.criador = :criador
              AND (CAST(:nome AS string) IS NULL OR CAST(:nome AS string) = '' OR LOWER(g.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
              AND (
                CAST(:usuario AS string) IS NULL OR CAST(:usuario AS string) = '' OR
                (c IS NOT NULL AND (LOWER(c.nome) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')))) OR
                (u IS NOT NULL AND (LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%'))))
              )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT g) FROM Grupo g
            LEFT JOIN GrupoXUsuario gxu ON gxu.grupo = g AND gxu.isAtivo = true
            LEFT JOIN gxu.usuario u
            LEFT JOIN g.criador c
            WHERE g.isAtivo = true
              AND g.criador = :criador
              AND (CAST(:nome AS string) IS NULL OR CAST(:nome AS string) = '' OR LOWER(g.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
              AND (
                CAST(:usuario AS string) IS NULL OR CAST(:usuario AS string) = '' OR
                (c IS NOT NULL AND (LOWER(c.nome) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')))) OR
                (u IS NOT NULL AND (LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%'))))
              )
            """)
    Page<Grupo> filtrarGrupos(
            @Param("criador") Usuario criador,
            @Param("nome") String nome,
            @Param("usuario") String usuario,
            Pageable pageable
    );
}
