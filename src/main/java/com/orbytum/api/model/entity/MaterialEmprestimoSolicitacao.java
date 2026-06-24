package com.orbytum.api.model.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class MaterialEmprestimoSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    private GrupoXUsuario usuario;

    @Nonnull
    @OneToMany
    private List<MaterialEmprestimoSolicitacaoItem> items;

    @Nonnull
    private String justificativa;

    @Nonnull
    private boolean isInterna;

    @Nonnull
    private boolean isAprovada;

    @Nonnull
    private LocalDateTime dthSolicitacao;

    @Nonnull
    private LocalDateTime dthResposta;

}
