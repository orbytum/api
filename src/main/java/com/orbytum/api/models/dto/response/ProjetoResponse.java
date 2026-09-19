package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.ProjetoStatus;

import java.time.LocalDateTime;

public record ProjetoResponse (
    Long id,
    Long grupoId,
    ProjetoStatus status,
    String titulo,
    String assunto,
    LocalDateTime dthRegistro,
    Boolean isAtivo
) {}
