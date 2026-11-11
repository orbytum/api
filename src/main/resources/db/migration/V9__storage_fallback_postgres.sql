-- Fallback de armazenamento: arquivos binários no PostgreSQL
-- Observação: o projeto está com spring.flyway.enabled=false. Aplique este
-- script manualmente ou habilite o Flyway antes do deploy.

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tamanho BIGINT,
    ADD COLUMN IF NOT EXISTS chave VARCHAR(500),
    ADD COLUMN IF NOT EXISTS dth_criacao TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW();

-- Converte a coluna de OID (mapeamento antigo de @Lob) para BYTEA.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'documento'
          AND column_name = 'documento'
          AND udt_name = 'oid'
    ) THEN
        ALTER TABLE documento
            ALTER COLUMN documento TYPE BYTEA USING lo_get(documento);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_documento_chave
    ON documento (chave)
    WHERE chave IS NOT NULL;

ALTER TABLE publicacao
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500);

ALTER TABLE evento
    ADD COLUMN IF NOT EXISTS ata_storage_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ata_documento_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_evento_ata_documento'
    ) THEN
        ALTER TABLE evento
            ADD CONSTRAINT fk_evento_ata_documento
            FOREIGN KEY (ata_documento_id) REFERENCES documento (id);
    END IF;
END $$;
