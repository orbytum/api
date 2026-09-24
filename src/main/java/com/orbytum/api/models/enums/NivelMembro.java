package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum NivelMembro {

    LIDER(3),
    COORDENADOR(2),
    PESQUISADOR(1);

    @Getter
    private final int prioridade;

    public boolean isPeloMenos(NivelMembro outro) {
        return this.prioridade >= outro.prioridade;
    }

    public boolean isLider() {
        return this == LIDER;
    }
}
