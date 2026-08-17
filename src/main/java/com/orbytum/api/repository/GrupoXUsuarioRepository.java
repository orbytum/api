package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import com.orbytum.api.models.entity.Grupo;

@Repository
public interface GrupoXUsuarioRepository extends JpaRepository<GrupoXUsuario, UUID> {

    @Query("SELECT gxu.role.permissoes FROM GrupoXUsuario gxu WHERE gxu.usuario = :usuario AND gxu.isAtivo = true")
    List<com.orbytum.api.models.enums.Permissao> findPermissoesByUsuario(@Param("usuario") Usuario usuario);

    boolean existsByGrupoAndUsuario(Grupo grupo, Usuario usuario);

    Optional<GrupoXUsuario> findByGrupoIdAndUsuarioIdAndIsAtivoTrue(Long grupoId, Long usuarioId);

    boolean existsByGrupoIdAndUsuarioIdAndIsAtivoTrue(Long grupoId, Long usuarioId);

    boolean existsByGrupoIdAndUsuarioEmailAndIsAtivoTrue(Long grupoId, String email);
}
