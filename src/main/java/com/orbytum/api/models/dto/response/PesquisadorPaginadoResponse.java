package com.orbytum.api.models.dto.response;

import java.util.List;

public record PesquisadorPaginadoResponse(
        List<PesquisadorResponse> items,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
