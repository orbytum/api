package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String nome;

    @Nonnull
    private String extensao;

    @Column(name = "content_type")
    private String contentType;

    private Long tamanho;

    @Column(unique = true, length = 500)
    private String chave;

    @Column(name = "dth_criacao")
    private LocalDateTime dthCriacao;

    @Nonnull
    @Column(columnDefinition = "bytea", nullable = false)
    private byte[] documento;

    @OneToOne(mappedBy = "documento", fetch = FetchType.LAZY)
    private Publicacao publicacao;

    @OneToOne(mappedBy = "ataDocumento", fetch = FetchType.LAZY)
    private Evento evento;
}
