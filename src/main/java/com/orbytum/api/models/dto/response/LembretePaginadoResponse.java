package com.orbytum.api.models.dto.response;

import java.util.List;
import java.util.Map;

public record LembretePaginadoResponse(
        List<LembreteResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        Map<String, Long> totaisPorTipo
) {}
