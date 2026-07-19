package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

@Entity
public class MaterialEmprestimoSolicitacaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Material material;

    @Nonnull
    private Integer quantidade;

}
