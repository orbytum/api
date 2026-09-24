package com.orbytum.api.repository;

import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjetoXUsuarioRepository extends JpaRepository<ProjetoXUsuario, UUID> {

    Optional<ProjetoXUsuario> findByProjetoIdAndUsuarioIdAndIsAtivoTrue(Long projetoId, Long usuarioId);

    boolean existsByProjetoIdAndUsuarioIdAndIsAtivoTrue(Long projetoId, Long usuarioId);

    List<ProjetoXUsuario> findAllByProjetoIdAndIsAtivoTrue(Long projetoId);
}
