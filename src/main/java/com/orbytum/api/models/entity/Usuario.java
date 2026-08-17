package com.orbytum.api.models.entity;

import java.time.LocalDateTime;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Setter
    private String nome;

    @Nonnull
    @Getter
    private String email;

    @Nonnull
    @Getter
    @Setter
    private String telefone;

    @Nonnull
    @Getter
    @Setter
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
