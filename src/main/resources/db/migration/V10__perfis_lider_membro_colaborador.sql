-- Níveis de grupo passam a ser LIDER, MEMBRO (exibido como "Pesquisador") e COLABORADOR (somente leitura).
UPDATE grupoxusuario SET nivel = 'MEMBRO' WHERE nivel IS NULL OR nivel IN ('PESQUISADOR', 'COORDENADOR');
UPDATE convite_grupo SET nivel = 'MEMBRO' WHERE nivel IS NULL OR nivel IN ('PESQUISADOR', 'COORDENADOR');

-- Líder de projeto passa a ser uma flag no vínculo projeto x usuário (no máximo um por projeto)
ALTER TABLE projeto_x_usuario DROP COLUMN IF EXISTS nivel;
ALTER TABLE projeto_x_usuario ADD COLUMN IF NOT EXISTS is_lider_projeto BOOLEAN NOT NULL DEFAULT false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_projeto_x_usuario_lider
    ON projeto_x_usuario (projeto_id)
    WHERE is_lider_projeto AND is_ativo;
