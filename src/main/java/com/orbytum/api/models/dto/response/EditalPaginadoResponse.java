package com.orbytum.api.models.dto.response;

import java.util.List;

public record EditalPaginadoResponse(
        List<EditalResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
