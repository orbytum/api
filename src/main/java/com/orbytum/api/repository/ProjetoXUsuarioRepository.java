package com.orbytum.api.repository;

import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.enums.NivelMembro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjetoXUsuarioRepository extends JpaRepository<ProjetoXUsuario, UUID> {

    Optional<ProjetoXUsuario> findByProjetoIdAndUsuarioIdAndIsAtivoTrue(Long projetoId, Long usuarioId);

    boolean existsByProjetoIdAndUsuarioIdAndIsAtivoTrue(Long projetoId, Long usuarioId);

    List<ProjetoXUsuario> findAllByProjetoIdAndIsAtivoTrue(Long projetoId);

    List<ProjetoXUsuario> findAllByUsuarioIdAndIsAtivoTrue(Long usuarioId);

    Optional<ProjetoXUsuario> findFirstByProjetoIdAndNivelAndIsAtivoTrue(Long projetoId, NivelMembro nivel);

    List<ProjetoXUsuario> findAllByProjetoIdAndNivelAndIsAtivoTrue(Long projetoId, NivelMembro nivel);

    long countByProjetoIdAndNivelAndIsAtivoTrue(Long projetoId, NivelMembro nivel);

    @Query("SELECT p.grupo.id FROM Projeto p WHERE p.id = :projetoId")
    Optional<Long> findGrupoIdByProjetoId(@Param("projetoId") Long projetoId);
}
