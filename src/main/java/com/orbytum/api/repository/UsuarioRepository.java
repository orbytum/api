package com.orbytum.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orbytum.api.models.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    /** Lista usuários ativos; administradores não são listados. `busca` nunca deve ser nulo (use "" para sem filtro): parâmetro String nulo vira bytea no PostgreSQL. */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.isAtivo = true
              AND (:busca = ''
               OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND NOT EXISTS (
                  SELECT 1 FROM CredenciaisLogin c
                  WHERE c.email = u.email
                    AND c.accessLevel <> com.orbytum.api.models.enums.AccessLevel.USER)
            """)
    Page<Usuario> buscar(@Param("busca") String busca, Pageable pageable);
}
