ALTER TABLE role ADD COLUMN IF NOT EXISTS is_lider BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE grupoxusuario ADD COLUMN IF NOT EXISTS nivel VARCHAR(20);
ALTER TABLE grupoxusuario ADD COLUMN IF NOT EXISTS funcao VARCHAR(20);

ALTER TABLE convite_grupo ADD COLUMN IF NOT EXISTS nivel VARCHAR(20);
ALTER TABLE convite_grupo ADD COLUMN IF NOT EXISTS email_convidado VARCHAR(255);

UPDATE grupoxusuario gxu
SET nivel = CASE WHEN r.is_lider THEN 'LIDER' ELSE 'PESQUISADOR' END
FROM role r
WHERE gxu.role_id = r.id AND gxu.nivel IS NULL;

UPDATE grupoxusuario SET nivel = 'PESQUISADOR' WHERE nivel IS NULL;
UPDATE convite_grupo SET nivel = 'PESQUISADOR' WHERE nivel IS NULL;
