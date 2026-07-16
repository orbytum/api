package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class MaterialEmprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    private Grupo grupo;

    @Nonnull
    private Integer quantidade;

    @Nonnull
    private boolean isDevolvido;

    @Nonnull
    private LocalDateTime dthInicio;

    @Nullable
    private LocalDateTime dthFim;

}
