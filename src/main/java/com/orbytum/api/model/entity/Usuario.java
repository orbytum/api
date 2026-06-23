package com.orbytum.api.model.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table
public class Usuario {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    @Getter
    private String nome;

    @Nonnull
    @Getter
    private String email;

    @Nonnull
    @Getter
    private String telefone;

    @Nonnull
    @Getter
    private String titulo;

}
