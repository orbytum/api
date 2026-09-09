package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;

public record ConviteCadastroDetalheResponse(
        Long id,
        String email,
        String token,
        String urlConvite,
        LocalDateTime dthRegistro,
        LocalDateTime dthExpiracao,
        boolean ativo
) { }
