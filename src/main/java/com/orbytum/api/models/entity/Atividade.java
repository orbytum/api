package com.orbytum.api.models.entity;

import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AtividadeStatus;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Projeto projeto;

    @Nonnull
    private String titulo;

    @Nonnull
    private String descricao;

    @Nonnull
    private AtividadeStatus status;

    @Nonnull
    private LocalDateTime dthPrazo;

    @Nonnull
    private LocalDateTime dthConclusao;

    @Nonnull
    private LocalDate dthRegistro;
    @Nonnull
    private boolean isAtivo;

}
