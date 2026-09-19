package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.ProjetoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjetoRequest (
    @NotNull(message = "O grupo do projeto é obrigatório")
    Long grupoId,

    /**
     * Status Pode ser Planejado ou Em_Andamento
     * No momento da criação colocar um check box (atividade ainda em planejamento)
     * @version 1.0
     */
    @NotNull(message = "O status do projeto é obrigatório")
    ProjetoStatus status,

    @NotBlank(message = "O título do projeto é obrigatório")
    String titulo,

    @NotBlank(message = "O assunto do projeto é obrigatório")
    String assunto
) {}
