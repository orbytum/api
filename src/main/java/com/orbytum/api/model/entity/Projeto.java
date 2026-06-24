package com.orbytum.api.model.entity;

import com.orbytum.api.configuration.data.converter.ProjetoStatusAttributeConverter;
import com.orbytum.api.model.enums.ProjetoStatus;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    private Grupo grupo;

    @Nonnull
    @Convert(converter = ProjetoStatusAttributeConverter.class)
    private ProjetoStatus status;

    @Nonnull
    private String titulo;

    @Nonnull
    private String assunto;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

}
