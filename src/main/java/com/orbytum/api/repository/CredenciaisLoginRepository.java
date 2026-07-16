package com.orbytum.api.repository;

import com.orbytum.api.models.entity.CredenciaisLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredenciaisLoginRepository extends JpaRepository<CredenciaisLogin, Integer> {

    Optional<CredenciaisLogin> findByEmail(String email);

    boolean existsByEmail(String email);
}
