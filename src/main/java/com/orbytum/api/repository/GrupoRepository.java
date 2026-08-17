package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    
    boolean existsByNome(String nome);

    Optional<Grupo> findByNome(String nome);                                                                                                                                                                 
                                                                                                                                                                                                                                                                                                                                                                        
    List<Grupo> findAllByIsAtivoTrue();
}
