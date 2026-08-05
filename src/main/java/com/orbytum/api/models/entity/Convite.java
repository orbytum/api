package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_convidado_id")
    private Usuario usuarioConvidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_remetente_id")
    private Usuario usuarioRemetente;

    @Nonnull
    private LocalDateTime dthExpiracao;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    public Convite(Grupo grupo, Usuario usuarioConvidado, Usuario usuarioRemetente, LocalDateTime dthExpiracao) {
        this.grupo = grupo;
        this.usuarioConvidado = usuarioConvidado;
        this.usuarioRemetente = usuarioRemetente;
        this.dthRegistro = LocalDateTime.now();
        this.dthExpiracao = dthExpiracao;
        this.isAtivo = true;
    }
}
