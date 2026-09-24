package com.orbytum.api.models.entity;

import com.orbytum.api.models.converter.AtividadeStatusAttributeConverter;
import com.orbytum.api.models.enums.AtividadeStatus;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_pai_id")
    private Atividade atividadePai;

    @OneToMany(mappedBy = "atividadePai", fetch = FetchType.LAZY)
    private List<Atividade> atividadesFilhas;

    @Nonnull
    private String titulo;

    @Nonnull
    private String descricao;

    @Nonnull
    @Convert(converter = AtividadeStatusAttributeConverter.class)
    private AtividadeStatus status;

    @Nonnull
    private LocalDateTime dthPrazo;

    private LocalDateTime dthConclusao;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    public Atividade(Projeto projeto, Usuario responsavel, Atividade atividadePai, String titulo, String descricao, LocalDateTime dthPrazo) {
        this.projeto = projeto;
        this.responsavel = responsavel;
        this.atividadePai = atividadePai;
        this.titulo = titulo;
        this.descricao = descricao;
        this.dthPrazo = dthPrazo;
        this.status = AtividadeStatus.PENDENTE;
        this.dthRegistro = LocalDateTime.now();
        this.isAtivo = true;
    }
}
