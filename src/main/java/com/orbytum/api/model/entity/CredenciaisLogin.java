package com.orbytum.api.model.entity;

import com.orbytum.api.model.enums.AccessLevel;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CredenciaisLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String email;

    @Nonnull
    private String senha;

    private boolean isAtivo;

    @Nonnull
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    @OneToOne(mappedBy = "credenciaisLogin")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criadorId")
    private Usuario criador;

    public CredenciaisLogin(
            @Nonnull String email,
            @Nonnull String senha,
            @Nonnull AccessLevel accessLevel,
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
