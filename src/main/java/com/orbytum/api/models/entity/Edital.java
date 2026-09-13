package com.orbytum.api.models.entity;

import com.orbytum.api.models.entity.joinColumns.EditalXProjeto;
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
    private Long id;

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

    @OneToMany(mappedBy = "edital", fetch = FetchType.LAZY)
    private List<EditalXProjeto> editaisProjetos;

    @Nonnull
    private boolean isAtivo;

}
