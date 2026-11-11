package com.orbytum.api.models.dto.response;

import java.util.List;

public record PublicacaoPaginadaResponse(
        List<PublicacaoResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
