package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialEmprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    private Grupo grupo;

    @Nonnull
    private Integer quantidade;

    @Nonnull
    private boolean isDevolvido;

    @Nonnull
    private LocalDateTime dthInicio;

    @Nullable
    private LocalDateTime dthFim;

}
