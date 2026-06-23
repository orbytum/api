package com.orbytum.api.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Permissao {
    PROJETO_CONVITE_CRIAR("projeto.convite.criar"),
    PROJETO_REUNIAO_AGENDAR("projeto.reuniao.agendar");

    @Getter private String key;

    public static Permissao fromKey(String key) {
        for (Permissao p : Permissao.values()) {
            if (p.getKey().equals(key)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Chave de permissão inválida: " + key);
    }
}
