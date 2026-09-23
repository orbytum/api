package com.orbytum.api.models.dto.request;

import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateLembreteRequest {

    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    private String descricao;

    @NotNull(message = "O tipo do lembrete é obrigatório")
    private TipoLembrete tipo;

    @NotNull(message = "A data e horário são obrigatórios")
    private LocalDateTime dataHora;

    private String localizacao;

    private String link;

    private TipoRecorrencia recorrencia = TipoRecorrencia.NENHUMA;

    @NotNull(message = "O ID do grupo é obrigatório")
    private Long grupoId;

    private List<Long> participantesIds;
}
