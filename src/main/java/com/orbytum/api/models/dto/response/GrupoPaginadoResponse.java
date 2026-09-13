package com.orbytum.api.models.dto.response;

import java.util.List;

public record GrupoPaginadoResponse(
        List<GrupoResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
