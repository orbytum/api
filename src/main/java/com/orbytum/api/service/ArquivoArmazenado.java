package com.orbytum.api.service;

import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.enums.StorageProvider;

/**
 * Resultado de uma operação de armazenamento.
 *
 * @param documento preenchido apenas quando o arquivo foi salvo no PostgreSQL
 */
public record ArquivoArmazenado(
        StorageProvider provedor,
        String chave,
        String url,
        String nomeArquivo,
        String contentType,
        long tamanho,
        Documento documento
) {
}
