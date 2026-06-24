package com.orbytum.api.model.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
public class Grupo {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String nome;

    @Nonnull
    private boolean isAtivo;

}
