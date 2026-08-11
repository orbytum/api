package com.orbytum.api.model.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String nome;

    @Nonnull
    private String email;

    @Nonnull
    private String telefone;

    @Nonnull
    private String titulo;

    @Nonnull
    private LocalDateTime dthRegistro;

    @Nonnull
    private boolean isAtivo;

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
