package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @Getter
    private String nome;

    @Nonnull
    @Getter
    private String email;

    @Nonnull
    @Getter
    private String telefone;

    @Nonnull
    @Getter
    private String titulo;

    @Nonnull
    @Getter
    private LocalDateTime dthRegistro;

    @Nonnull
    @Getter
    private boolean isAtivo;

    @Nonnull
    @Getter
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "credenciaisLoginId")
    private CredenciaisLogin credenciaisLogin;

    public Usuario(
            @Nonnull String nome,
            @Nonnull String email,
            @Nonnull String telefone,
            @Nonnull String titulo
    ) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.titulo = titulo;
        this.dthRegistro = LocalDateTime.now();
        this.isAtivo = true;
    }
}
