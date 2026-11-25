package com.orbytum.api.models.dto.response;

import java.util.List;

public record UsuarioPaginadoResponse(
        List<UsuarioResponse> items,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
