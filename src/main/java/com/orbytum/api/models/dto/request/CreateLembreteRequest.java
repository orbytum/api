package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateLembreteRequest(
        Long grupoId,

        @NotBlank(message = "O título do lembrete é obrigatório.")
        String titulo,

        String descricao,

        @NotNull(message = "O tipo do lembrete é obrigatório.")
        TipoLembrete tipo,

        @NotNull(message = "A data e horário do lembrete são obrigatórios.")
        LocalDateTime dataHora,

        String local,
        String localizacao,
        String link,
        TipoRecorrencia recorrencia,
        String organizador,
        List<String> participantes,
        List<Long> participanteIds
) {
    public String getLocalFinal() {
        if (localizacao != null && !localizacao.isBlank()) {
            return localizacao.trim();
        }
        if (local != null && !local.isBlank()) {
            return local.trim();
        }
        return null;
    }
}
