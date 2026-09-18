package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid")
    private UUID uuid;

    @Nonnull
    private String titulo;

    @Nonnull
    private String descricao;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    private Projeto projeto;

    @Nonnull
    private String url;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "documentoId", referencedColumnName = "id")
    private Documento documento;

}
