package com.orbytum.api.service;

public record ArquivoDownload(
        byte[] bytes,
        String nomeArquivo,
        String contentType
) {
}
