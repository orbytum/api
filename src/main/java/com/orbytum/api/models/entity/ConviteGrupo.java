package com.orbytum.api.models.entity;

import com.orbytum.api.models.enums.TipoConvite;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "convite_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConviteGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_convidado_id")
    private Usuario usuarioConvidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_remetente_id")
    private Usuario usuarioRemetente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ConviteXProjeto",
            joinColumns = @JoinColumn(name = "convite_id"),
            inverseJoinColumns = @JoinColumn(name = "projeto_id")
    )
    private List<Projeto> projetos;

    private Integer limiteUso;

    @Builder.Default
    private Integer usos = 0;

    @Nonnull
    private LocalDateTime dthExpiracao;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    public ConviteGrupo(Grupo grupo, Usuario usuarioConvidado, Usuario usuarioRemetente, List<Projeto> projetos, LocalDateTime dthExpiracao) {
        this.grupo = grupo;
        this.usuarioConvidado = usuarioConvidado;
        this.usuarioRemetente = usuarioRemetente;
        this.projetos = projetos;
        this.dthRegistro = LocalDateTime.now();
        this.dthExpiracao = dthExpiracao;
        this.usos = 0;
        this.isAtivo = true;
    }

    public ConviteGrupo(Grupo grupo, Usuario usuarioRemetente, String token, List<Projeto> projetos, LocalDateTime dthExpiracao) {
        this(grupo, usuarioRemetente, token, projetos, dthExpiracao, null, null);
    }

    public ConviteGrupo(Grupo grupo, Usuario usuarioRemetente, String token, List<Projeto> projetos, LocalDateTime dthExpiracao, Role role, Integer limiteUso) {
        this.grupo = grupo;
        this.usuarioRemetente = usuarioRemetente;
        this.token = token;
        this.projetos = projetos;
        this.dthRegistro = LocalDateTime.now();
        this.dthExpiracao = dthExpiracao;
        this.role = role;
        this.limiteUso = limiteUso;
        this.usos = 0;
        this.isAtivo = true;
    }
}

