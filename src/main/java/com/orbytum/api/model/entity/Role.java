package com.orbytum.api.model.entity;

import com.orbytum.api.configuration.data.converter.PermissaoAttributeConverter;
import com.orbytum.api.model.enums.Permissao;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
@Table
public class Role {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    @Getter
    private String nome;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "RoleXPermissao")
    @Convert(converter = PermissaoAttributeConverter.class)
    private List<Permissao> permissoes;

}
