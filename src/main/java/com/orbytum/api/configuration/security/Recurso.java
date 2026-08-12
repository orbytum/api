package com.orbytum.api.configuration.security;

import com.orbytum.api.models.enums.Permissao;

public enum Recurso {

    PROJETO("projeto"),
    ATIVIDADE("atividade"),
    MATERIAL("material"),
    USUARIO("usuario"),
    GRUPO("grupo"),
    CONVITE("convite"),
    PUBLICACAO("publicacao"),
    EVENTO("evento"),
    EDITAL("edital");

    private final String key;

    Recurso(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean hasPermissao(Permissao permissao) {
        return permissao.getKey().startsWith(this.key + ".");
    }
}
