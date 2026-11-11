package com.orbytum.api.models.dto.response;

public record ParticipanteLembreteResponse(
        Long id,
        Long usuarioId,
        String nome,
        boolean presencaConfirmada
) {}
