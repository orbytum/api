package com.orbytum.api.models.dto.response;

import java.util.List;

public record ConviteCadastroPaginadoResponse(
        List<ConviteCadastroDetalheResponse> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        long totalAtivos,
        long totalInativos,
        long totalGeral
) {}
