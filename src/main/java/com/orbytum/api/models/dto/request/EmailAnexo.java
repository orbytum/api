package com.orbytum.api.models.dto.request;

import org.springframework.core.io.InputStreamSource;

public record EmailAnexo(
        String nomeArquivo,
        InputStreamSource conteudo,
        String contentType
) {}
