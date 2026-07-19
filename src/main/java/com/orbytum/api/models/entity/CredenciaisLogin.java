package com.orbytum.api.models.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class CredenciaisLogin {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @Getter
    private String email;

    @Nonnull
    @Getter
    private String senha;

    @Getter
    private boolean isAtivo;

    @Nonnull
    @Getter
    @Enumerated(EnumType.STRING)
    private com.orbytum.api.models.enums.AccessLevel accessLevel;

    @Nonnull
    @Getter
    @OneToOne(mappedBy = "credenciaisLogin")
    private Usuario usuario;

    @Getter
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "criadorId")
    private Usuario criador;

    public CredenciaisLogin(
            @Nonnull String email,
            @Nonnull String senha,
            @Nonnull com.orbytum.api.models.enums.AccessLevel accessLevel,
            @Nonnull Usuario usuario,
            Usuario criador
    ) {
        this.email = email;
        this.senha = senha;
        this.accessLevel = accessLevel;
        this.usuario = usuario;
        this.criador = criador;
        this.isAtivo = true;
    }
}
