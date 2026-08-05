package com.orbytum.api.models.entity;

import com.orbytum.api.models.converter.ProjetoStatusAttributeConverter;
import com.orbytum.api.models.entity.joinColumns.EditalXProjeto;
import com.orbytum.api.models.enums.ProjetoStatus;
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
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    private Grupo grupo;

    @Nonnull
    @Convert(converter = ProjetoStatusAttributeConverter.class)
    private ProjetoStatus status;

    @Nonnull
    private String titulo;

    @Nonnull
    private String assunto;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    @OneToMany(mappedBy = "projeto", fetch = FetchType.LAZY)
    private List<EditalXProjeto> editaisProjetos;

}

