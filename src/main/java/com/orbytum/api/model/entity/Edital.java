package com.orbytum.api.model.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Edital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String titulo;

    @Nonnull
    private String descricao;

    @Nonnull
    private String url;

    @Nullable
    private BigDecimal valor;

    @Nonnull
    private LocalDateTime dthAbertura;

    @Nonnull
    private LocalDateTime dthEncerramento;

    @ManyToMany(fetch = FetchType.LAZY)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "EditalXProjeto")
    private List<Projeto> projetos;

    @Nonnull
    private boolean isAtivo;

}
