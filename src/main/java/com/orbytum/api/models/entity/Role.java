package com.orbytum.api.models.entity;

import com.orbytum.api.models.converter.PermissaoAttributeConverter;
import com.orbytum.api.models.enums.Permissao;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
public class Role {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @Getter
    private String nome;

    @Getter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "RoleXPermissao")
    @Convert(converter = PermissaoAttributeConverter.class)
    private List<Permissao> permissoes;

}
