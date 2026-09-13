package com.orbytum.api.models.entity;

import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class MaterialEmprestimoSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
