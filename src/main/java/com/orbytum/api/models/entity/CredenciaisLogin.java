package com.orbytum.api.models.entity;

import com.orbytum.api.models.enums.AccessLevel;
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
    private AccessLevel accessLevel;

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
