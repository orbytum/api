package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class Grupo {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Nonnull
    private String nome;

    @Getter
    @Setter
    @Nonnull
    private boolean isAtivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criadorId")
    @Getter
    private Usuario criador;

    public Grupo(String nome, Usuario criador) {
        this.nome = nome;
        this.isAtivo = true;
        this.criador = criador;
    }
}
