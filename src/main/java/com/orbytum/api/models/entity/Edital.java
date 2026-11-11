package com.orbytum.api.models.entity;

import com.orbytum.api.models.entity.joinColumns.EditalXGrupo;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "edital_x_projeto",
            joinColumns = @JoinColumn(name = "edital_id"),
            inverseJoinColumns = @JoinColumn(name = "projeto_id"))
    @Builder.Default
    private Set<Projeto> projetos = new HashSet<>();
}
