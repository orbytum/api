package com.orbytum.api.repository;

import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.enums.AccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredenciaisLoginRepository extends JpaRepository<CredenciaisLogin, Long> {

    Optional<CredenciaisLogin> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByAccessLevel(AccessLevel accessLevel);

    Optional<CredenciaisLogin> findByAccessLevel(AccessLevel accessLevel);

    long countByAccessLevel(AccessLevel accessLevel);
}
