package com.orbytum.api.models.entity.joinColumns;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrupoXUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Role role;

    @Nonnull
    private boolean isAtivo;

    public GrupoXUsuario(@Nonnull Grupo grupo, @Nonnull Usuario usuario, @Nonnull Role role, boolean isAtivo) {
        this.grupo = grupo;
        this.usuario = usuario;
        this.role = role;
        this.isAtivo = isAtivo;
    }
}
