package com.orbytum.api.models.entity;

import com.orbytum.api.configuration.data.converter.MaterialStatusAttributeConverter;
import com.orbytum.api.models.enums.MaterialStatus;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String nome;

    @Nonnull
    private String descricao;

    @Nonnull
    private String identificador;

    @Nonnull
    private Integer quantidade;

    @Nonnull
    private String localizacao;

    @Nonnull
    @Convert(converter = MaterialStatusAttributeConverter.class)
    private MaterialStatus status;

    @Nonnull
    private LocalDateTime dthCompra;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

    @OneToMany(fetch = FetchType.LAZY)
    private List<MaterialEmprestimo> emprestimos;

}
