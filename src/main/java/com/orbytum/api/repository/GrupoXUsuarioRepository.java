package com.orbytum.api.repository;

import com.orbytum.api.models.entity.GrupoXUsuario;
import com.orbytum.api.models.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoXUsuarioRepository extends JpaRepository<GrupoXUsuario, java.util.UUID> {

    @Query("SELECT gxu.role.permissoes FROM GrupoXUsuario gxu WHERE gxu.usuario = :usuario AND gxu.isAtivo = true")
    List<com.orbytum.api.models.enums.Permissao> findPermissoesByUsuario(@Param("usuario") Usuario usuario);
}
