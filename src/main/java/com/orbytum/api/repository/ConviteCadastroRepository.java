package com.orbytum.api.repository;

import com.orbytum.api.models.entity.ConviteCadastro;
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
public interface ConviteCadastroRepository extends JpaRepository<ConviteCadastro, Long> {
    Optional<ConviteCadastro> findByEmailAndIsAtivoTrue(String email);
    Optional<ConviteCadastro> findByTokenAndIsAtivoTrue(String token);
    List<ConviteCadastro> findAllByOrderByDthRegistroDesc();

    @Query(
        value = """
            SELECT c FROM ConviteCadastro c
            WHERE (:email IS NULL OR :email = '' OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (
                (:status = 'ativos' AND c.isAtivo = true AND c.dthExpiracao > :now)
                OR (:status = 'inativos' AND (c.isAtivo = false OR c.dthExpiracao <= :now))
                OR (:status = 'todos' OR :status IS NULL OR :status = '')
              )
            ORDER BY c.dthRegistro DESC
        """,
        countQuery = """
            SELECT COUNT(c) FROM ConviteCadastro c
            WHERE (:email IS NULL OR :email = '' OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (
                (:status = 'ativos' AND c.isAtivo = true AND c.dthExpiracao > :now)
                OR (:status = 'inativos' AND (c.isAtivo = false OR c.dthExpiracao <= :now))
                OR (:status = 'todos' OR :status IS NULL OR :status = '')
              )
        """
    )
    Page<ConviteCadastro> filtrarConvites(
        @Param("email") String email,
        @Param("status") String status,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM ConviteCadastro c WHERE c.isAtivo = true AND c.dthExpiracao > :now")
    long countAtivos(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(c) FROM ConviteCadastro c WHERE c.isAtivo = false OR c.dthExpiracao <= :now")
    long countInativos(@Param("now") LocalDateTime now);
}
