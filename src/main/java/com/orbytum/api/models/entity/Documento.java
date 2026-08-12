package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Nonnull
    @Getter
    private String nome;
    @Nonnull
    @Getter
    private String extensao;
    @Lob
    @Nonnull
    private byte[] documento;

    @OneToOne(mappedBy = "documento")
    private Publicacao publicacao;
}
