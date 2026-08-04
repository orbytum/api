package com.orbytum.api.service;

import com.orbytum.api.models.entity.CredenciaisLogin;
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

    @Transactional
    public CredenciaisLogin save(CredenciaisLogin credenciais) {
        return credenciaisLoginRepository.save(credenciais);
    }
}
