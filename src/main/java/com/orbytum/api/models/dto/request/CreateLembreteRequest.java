package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Size;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateLembreteRequest(
        Long grupoId,

        @NotBlank(message = "O título do lembrete é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
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
        List<Long> participanteIds,
        Long projetoId,
        Boolean paraGrupoInteiro
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
