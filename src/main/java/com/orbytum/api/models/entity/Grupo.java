package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String nome;

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

    public Grupo(@Nonnull String nome, boolean isAtivo) {
        this.nome = nome;
        this.isAtivo = isAtivo;
    }
}
