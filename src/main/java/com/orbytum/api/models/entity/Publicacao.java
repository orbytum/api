package com.orbytum.api.models.entity;

import com.orbytum.api.models.enums.StorageProvider;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "documento_id", referencedColumnName = "id")
    private Documento documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", length = 20)
    private StorageProvider storageProvider;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    /** REQ-088: tipo da publicação (ex.: Artigo, Livro, Tese). */
    @Column(name = "tipo", length = 20)
    private String tipo;

    /** REQ-088: data em que a publicação foi publicada (difere da data de cadastro). */
    @Column(name = "data_publicacao")
    private LocalDate dataPublicacao;

    /** REQ-088: link externo da publicação (ex.: DOI ou página do periódico). */
    @Column(name = "link", length = 500)
    private String link;

}
