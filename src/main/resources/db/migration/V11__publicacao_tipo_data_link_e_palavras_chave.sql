-- REQ-088: tipo, data da publicação e link externo nas publicações
ALTER TABLE publicacao ADD COLUMN IF NOT EXISTS tipo VARCHAR(20);
ALTER TABLE publicacao ADD COLUMN IF NOT EXISTS data_publicacao DATE;
ALTER TABLE publicacao ADD COLUMN IF NOT EXISTS link VARCHAR(500);

-- REQ-060: palavras-chave dos temas do grupo de pesquisa, usadas para filtrar editais
CREATE TABLE IF NOT EXISTS grupo_palavra_chave (
    grupo_id BIGINT      NOT NULL REFERENCES grupo (id),
    palavra  VARCHAR(60) NOT NULL,
    PRIMARY KEY (grupo_id, palavra)
);
