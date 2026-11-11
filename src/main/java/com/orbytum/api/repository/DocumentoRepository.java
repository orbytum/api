package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findByChave(String chave);
}
