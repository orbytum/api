package com.orbytum.api.models.entity.joinColumns;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
public class GrupoXUsuario {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    @Getter
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    @Getter
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @Getter
    private Role role;

    @Nonnull
    @Getter
    private boolean isAtivo;

    public GrupoXUsuario(@Nonnull Grupo grupo, @Nonnull Usuario usuario, Role role) {
        this.grupo = grupo;
        this.usuario = usuario;
        this.role = role;
        this.isAtivo = true;
    }
}
