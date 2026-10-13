package com.orbytum.api.models.dto.response;

import java.util.List;

public record NoticiaPaginadaResponse(
        List<NoticiaResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
