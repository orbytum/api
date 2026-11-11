package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.entity.Projeto;

public record ProjetoRotuloResponse(Long id, String titulo) {

    public static ProjetoRotuloResponse fromEntity(Projeto projeto) {
        if (projeto == null) {
            return null;
        }
        return new ProjetoRotuloResponse(projeto.getId(), projeto.getTitulo());
    }
}
