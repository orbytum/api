package com.orbytum.api.models.entity;

import com.orbytum.api.models.enums.TipoConvite;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @Column(unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    private TipoConvite tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_convidado_id")
    private Usuario usuarioConvidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_remetente_id")
    private Usuario usuarioRemetente;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ConviteXProjeto",
            joinColumns = @JoinColumn(name = "convite_id"),
            inverseJoinColumns = @JoinColumn(name = "projeto_id")
    )
    private List<Projeto> projetos;

    @Nonnull
    private LocalDateTime dthExpiracao;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    public Convite(Grupo grupo, Usuario usuarioConvidado, Usuario usuarioRemetente, List<Projeto> projetos, LocalDateTime dthExpiracao) {
        this.grupo = grupo;
        this.usuarioConvidado = usuarioConvidado;
        this.usuarioRemetente = usuarioRemetente;
        this.projetos = projetos;
        this.tipo = TipoConvite.DIRETO;
        this.dthRegistro = LocalDateTime.now();
        this.dthExpiracao = dthExpiracao;
        this.isAtivo = true;
    }

    public Convite(Grupo grupo, Usuario usuarioRemetente, String token, List<Projeto> projetos, LocalDateTime dthExpiracao) {
        this.grupo = grupo;
        this.usuarioRemetente = usuarioRemetente;
        this.token = token;
        this.tipo = TipoConvite.LINK_EXCLUSIVO;
        this.projetos = projetos;
        this.dthRegistro = LocalDateTime.now();
        this.dthExpiracao = dthExpiracao;
        this.isAtivo = true;
    }
}

