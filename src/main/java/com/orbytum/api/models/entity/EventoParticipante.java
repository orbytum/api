package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evento_participante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Nonnull
    @Column(nullable = false)
    private String nome;

    @Column(name = "presenca_confirmada", nullable = false)
    @Builder.Default
    private boolean presencaConfirmada = false;
}
