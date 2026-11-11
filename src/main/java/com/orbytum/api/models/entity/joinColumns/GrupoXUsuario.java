package com.orbytum.api.models.entity.joinColumns;

import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.NivelMembro;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrupoXUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelMembro nivel = NivelMembro.PESQUISADOR;

    @Column(length = 20)
    private String funcao;

    @Nonnull
    private boolean isAtivo;

    public GrupoXUsuario(@Nonnull Grupo grupo, @Nonnull Usuario usuario, Role role) {
        this(grupo, usuario, role, NivelMembro.PESQUISADOR, null, true);
    }

    public GrupoXUsuario(@Nonnull Grupo grupo, @Nonnull Usuario usuario, @Nonnull Role role, boolean isAtivo) {
        this(grupo, usuario, role, NivelMembro.PESQUISADOR, null, isAtivo);
    }

    public GrupoXUsuario(@Nonnull Grupo grupo, @Nonnull Usuario usuario, Role role, NivelMembro nivel, String funcao, boolean isAtivo) {
        this.grupo = grupo;
        this.usuario = usuario;
        this.role = role;
        this.nivel = nivel != null ? nivel : NivelMembro.PESQUISADOR;
        this.funcao = funcao;
        this.isAtivo = isAtivo;
    }
}
