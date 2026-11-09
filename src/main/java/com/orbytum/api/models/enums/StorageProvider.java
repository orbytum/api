package com.orbytum.api.models.enums;

/**
 * Provedor utilizado para armazenar um arquivo.
 *
 * <p>AUTO representa a decisão automática: tenta o S3 quando configurado e,
 * em caso de falha, usa o PostgreSQL se o fallback estiver habilitado.</p>
 */
public enum StorageProvider {
    AUTO,
    S3,
    DATABASE
}
