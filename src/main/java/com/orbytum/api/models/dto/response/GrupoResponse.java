package com.orbytum.api.models.dto.response;

public record GrupoResponse(
        Long id,
        String nome,
        Boolean isAtivo,
        String nomeLider,
        Long idLider,
        Integer totalParticipantes
) {
    public GrupoResponse(Long id, String nome, Boolean isAtivo) {
        this(id, nome, isAtivo, null, null, 0);
    }
}
