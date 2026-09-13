package com.orbytum.api.models.dto.generico;

public record ErroResponse(
        int status,
        String mensagem,
        long timestamp
) {}