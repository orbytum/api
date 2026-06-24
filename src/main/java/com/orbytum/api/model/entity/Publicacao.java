package com.orbytum.api.model.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Publicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String titulo;

    @Nonnull
    private String descricao;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    private Projeto projeto;

    @Nonnull
    private String url;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

}
