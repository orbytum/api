package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.StatusLembrete;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;

import java.time.LocalDateTime;
import java.util.List;

public record LembreteResponse(
        Long id,
        Long grupoId,
        String grupo,
        Long criadorId,
        String criadorNome,
        String titulo,
        String descricao,
        TipoLembrete tipo,
        LocalDateTime dataHora,
        LocalDateTime proximaOcorrencia,
        String local,
        String localizacao,
        String link,
        TipoRecorrencia recorrencia,
        String organizador,
        List<String> participantes,
        List<ParticipanteLembreteResponse> participantesDetalhes,
        StatusLembrete status,
        boolean presencaConfirmada,
        boolean temAta,
        String ataUrl,
        String ataNomeArquivo,
        LocalDateTime dthRegistro,
        boolean isAtivo
) {}
