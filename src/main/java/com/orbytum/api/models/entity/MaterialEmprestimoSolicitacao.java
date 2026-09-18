package com.orbytum.api.models.entity;

import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialEmprestimoSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private GrupoXUsuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    private Projeto projeto;

    @Nonnull
    @OneToMany(cascade = CascadeType.ALL)
    @Builder.Default
    private List<MaterialEmprestimoSolicitacaoItem> items = new ArrayList<>();

    @Nonnull
    private String justificativa;

    @Nonnull
    private boolean isInterna;

    @Nonnull
    private boolean isAprovada;

    @Nonnull
    private LocalDateTime dthSolicitacao;

    private LocalDateTime dthResposta;

}
