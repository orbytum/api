package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** REQ-060: palavras-chave dos temas do grupo, usadas para filtrar editais relevantes. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "grupo_palavra_chave", joinColumns = @JoinColumn(name = "grupo_id"))
    @Column(name = "palavra", nullable = false, length = 60)
    private Set<String> palavrasChave = new LinkedHashSet<>();

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
