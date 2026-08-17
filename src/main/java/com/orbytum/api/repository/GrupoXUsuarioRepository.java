package com.orbytum.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;

@Repository
public interface GrupoXUsuarioRepository extends JpaRepository<GrupoXUsuario, java.util.UUID> {
    @Query("SELECT gxu.role.permissoes FROM GrupoXUsuario gxu WHERE gxu.usuario = :usuario AND gxu.isAtivo = true")
    List<com.orbytum.api.models.enums.Permissao> findPermissoesByUsuario(@Param("usuario") Usuario usuario);

    boolean existsByGrupoAndUsuario(Grupo grupo, Usuario usuario);
}
