package com.orbytum.api.service;

import com.orbytum.api.model.entity.CredenciaisLogin;
import com.orbytum.api.model.enums.AccessLevel;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CredenciaisLoginService {

    private final CredenciaisLoginRepository credenciaisLoginRepository;

    public CredenciaisLoginService(CredenciaisLoginRepository credenciaisLoginRepository) {
        this.credenciaisLoginRepository = credenciaisLoginRepository;
    }

    public Optional<CredenciaisLogin> findByEmail(String email) {
        return credenciaisLoginRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return credenciaisLoginRepository.existsByEmail(email);
    }

    public boolean existsByAccessLevel(AccessLevel accessLevel) {
        return credenciaisLoginRepository.existsByAccessLevel(accessLevel);
    }

    public Optional<CredenciaisLogin> findByAccessLevel(AccessLevel accessLevel) {
        return credenciaisLoginRepository.findByAccessLevel(accessLevel);
    }

    public long countByAccessLevel(AccessLevel accessLevel) {
        return credenciaisLoginRepository.countByAccessLevel(accessLevel);
    }

    @Transactional
    public CredenciaisLogin save(CredenciaisLogin credenciais) {
        return credenciaisLoginRepository.save(credenciais);
    }

    @Transactional
    public void delete(CredenciaisLogin credenciais) {
        credenciaisLoginRepository.delete(credenciais);
    }
}
