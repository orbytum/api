package com.orbytum.api.models.entity;

import com.orbytum.api.models.entity.joinColumns.EditalXGrupo;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "edital")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @Column(nullable = false)
    private String organizador;

    @Nonnull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(precision = 15, scale = 2)
    private BigDecimal valor;

    @Nonnull
    @Column(nullable = false, length = 512)
    private String uri;

    @Column(name = "dth_abertura", nullable = false)
    private LocalDateTime dthAbertura;

    @Column(name = "dth_encerramento", nullable = false)
    private LocalDateTime dthEncerramento;

    @OneToMany(mappedBy = "edital", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EditalXGrupo> grupos = new ArrayList<>();
}
