package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String titulo;

    @Nonnull
    private String descricao;

    @Nonnull
    private boolean isOnline;

    @Nullable
    private String url;

    @Nullable
    private String localizacao;

    @Nonnull
    private LocalDateTime dthInicio;

    @Nonnull
    private LocalDateTime dthFim;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;


}
