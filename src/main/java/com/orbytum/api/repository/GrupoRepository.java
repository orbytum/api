package com.orbytum.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    
    boolean existsByNome(String nome);

    Optional<Grupo> findByNome(String nome);

    List<Grupo> findAllByIsAtivoTrue();

    List<Grupo> findAllByCriadorAndIsAtivoTrue(Usuario criador);
}
