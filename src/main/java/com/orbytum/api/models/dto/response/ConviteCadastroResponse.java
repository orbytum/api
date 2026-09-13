package com.orbytum.api.models.dto.response;

import java.time.LocalDateTime;

public record ConviteCadastroResponse(
        Long idConvite,
        String token,
        String urlConvite,
        LocalDateTime dthExpiracao
) { }

