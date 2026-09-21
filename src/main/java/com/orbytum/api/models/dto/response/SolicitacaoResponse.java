package com.orbytum.api.models.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitacaoResponse(
        Long id,
        String titulo,
        String descricao,
        String justificativa,
        boolean isInterna,
        boolean isAprovada,
        Long projetoId,
        String projetoTitulo,
        Long usuarioId,
        String usuarioNome,
        Integer quantidade,
        BigDecimal valor,
        LocalDateTime dthSolicitacao,
        LocalDateTime dthResposta
) {}
