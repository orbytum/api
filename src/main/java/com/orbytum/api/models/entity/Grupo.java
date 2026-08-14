package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class Grupo {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Nonnull
    private String nome;

    @Getter
    @Nonnull
    private boolean isAtivo;

    public Grupo(String nome) {
        this.nome = nome;
        this.isAtivo = true;
    }
}
