package com.orbytum.api.models.entity;

import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lembrete")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lembrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoLembrete tipo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "localizacao")
    private String localizacao;

    @Column(name = "link")
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "recorrencia", nullable = false, length = 50)
    @Builder.Default
    private TipoRecorrencia recorrencia = TipoRecorrencia.NENHUMA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lembrete_participante",
        joinColumns = @JoinColumn(name = "lembrete_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @Builder.Default
    private List<Usuario> participantes = new ArrayList<>();

    @Column(name = "ata_key", length = 512)
    private String ataKey;

    @Column(name = "ata_nome_original")
    private String ataNomeOriginal;

    @Column(name = "dth_criacao", nullable = false)
    private LocalDateTime dthCriacao;

    @Column(name = "is_ativo", nullable = false)
    @Builder.Default
    private boolean isAtivo = true;
}
