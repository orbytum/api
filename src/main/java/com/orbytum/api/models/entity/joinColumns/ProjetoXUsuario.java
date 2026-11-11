package com.orbytum.api.models.entity.joinColumns;

import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.NivelMembro;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoXUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelMembro nivel = NivelMembro.PESQUISADOR;

    @Nonnull
    private boolean isAtivo;

    public ProjetoXUsuario(@Nonnull Projeto projeto, @Nonnull Usuario usuario, NivelMembro nivel) {
        this.projeto = projeto;
        this.usuario = usuario;
        this.nivel = nivel != null ? nivel : NivelMembro.PESQUISADOR;
        this.isAtivo = true;
    }
}
