package com.orbytum.api.model.entity;

import com.orbytum.api.configuration.data.converter.PermissaoAttributeConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private Grupo grupo;

    @ManyToMany(fetch = FetchType.LAZY)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ConviteXProjeto")
    private List<Projeto> projetos;

    @Nonnull
    private LocalDateTime dthExpiracao;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

}
