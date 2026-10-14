package com.orbytum.api.models.entity;

import com.orbytum.api.models.converter.NoticiaStatusAttributeConverter;
import com.orbytum.api.models.enums.NoticiaStatus;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid")
    private UUID uuid;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovador_id")
    private Usuario aprovador;

    @Nonnull
    private String titulo;

    @Nonnull
    @Column(columnDefinition = "TEXT")
    private String conteudo;

    private String referencia;

    @Nonnull
    private String assunto;

    private LocalDateTime dataInicioExibicao;

    private LocalDateTime dataFimExibicao;

    private String motivoRejeicao;

    @Nonnull
    @Convert(converter = NoticiaStatusAttributeConverter.class)
    private NoticiaStatus status;

    @Nonnull
    private LocalDateTime dthRegistro;

    private LocalDateTime dthAprovacao;

    @Nonnull
    private boolean isAtivo;

    public Noticia(
            Grupo grupo,
            Usuario autor,
            String titulo,
            String conteudo,
            String referencia,
            String assunto,
            LocalDateTime dataInicioExibicao,
            LocalDateTime dataFimExibicao
    ) {
        this.uuid = UUID.randomUUID();
        this.grupo = grupo;
        this.autor = autor;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.referencia = referencia;
        this.assunto = assunto;
        this.dataInicioExibicao = dataInicioExibicao;
        this.dataFimExibicao = dataFimExibicao;
        this.status = NoticiaStatus.PENDENTE;
        this.dthRegistro = LocalDateTime.now();
        this.isAtivo = true;
    }
}
