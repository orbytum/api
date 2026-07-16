package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
public class GrupoXUsuario {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

}
