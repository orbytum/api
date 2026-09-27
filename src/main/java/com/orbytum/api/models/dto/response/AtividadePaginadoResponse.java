package com.orbytum.api.models.dto.response;

import java.util.List;

public record AtividadePaginadoResponse(
        List<AtividadeResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
