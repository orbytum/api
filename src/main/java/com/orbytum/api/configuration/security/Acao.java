package com.orbytum.api.configuration.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Acao {

    CRIAR("criar"),
    EDITAR("editar"),
    DELETAR("deletar"),
    LER("ler"),
    CONVIDAR("convidar"),
    AGENDAR("agendar");

    @Getter
    private final String key;
}
