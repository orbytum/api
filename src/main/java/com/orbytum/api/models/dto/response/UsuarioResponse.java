package com.orbytum.api.models.dto.response;

import com.orbytum.api.models.enums.AccessLevel;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        String titulo,
        AccessLevel accessLevel,
        boolean isAtivo,
        LocalDateTime dthRegistro
) {}
