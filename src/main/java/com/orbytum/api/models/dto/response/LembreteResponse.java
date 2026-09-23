package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;

import java.time.LocalDateTime;
import java.util.List;

public record LembreteResponse(
        Long id,
        String titulo,
        String descricao,
        TipoLembrete tipo,
        LocalDateTime dataHora,
        String localizacao,
        String link,
        TipoRecorrencia recorrencia,
        Long grupoId,
        String grupoNome,
        Long organizadorId,
        String organizadorNome,
        String organizadorEmail,
        List<ParticipanteLembreteResponse> participantes,
        String ataNomeOriginal,
        String ataUrl,
        boolean temAta,
        LocalDateTime dthCriacao
) {
    public record ParticipanteLembreteResponse(
            Long id,
            String nome,
            String email
    ) {}
}
