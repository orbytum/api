package com.orbytum.api.models.entity;

import com.orbytum.api.models.converter.PermissaoAttributeConverter;
import com.orbytum.api.models.enums.Permissao;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String nome;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "RoleXPermissao")
    @Convert(converter = PermissaoAttributeConverter.class)
    private List<Permissao> permissoes;

    @Nonnull
    private boolean isLider = true;

    public Role(@Nonnull String nome, List<Permissao> permissoes, boolean isLider) {
        this.nome = nome;
        this.permissoes = permissoes;
        this.isLider = isLider;
    }
}
