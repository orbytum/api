package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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
    @JoinColumn(name = "criador_id")
    @Getter
    private Usuario criador;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Projeto> projetos;

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
