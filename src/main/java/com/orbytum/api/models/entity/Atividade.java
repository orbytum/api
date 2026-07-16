package com.orbytum.api.models.entity;

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
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @ManyToMany(fetch = FetchType.LAZY)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "GrupoXUsuarioXProjetoXAtividade")
    private List<GrupoXUsuario> responsaveis;

    @Nonnull
    private boolean isAtivo;

}
