package com.orbytum.api.models.dto.response;

import java.util.List;

public record LembretePaginadoResponse(
        List<LembreteResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
