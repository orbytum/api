-- O papel do membro no grupo passa a ser definido apenas por grupoxusuario.nivel
-- (LIDER, COORDENADOR, PESQUISADOR). A entidade Role e suas tabelas deixam de existir.
UPDATE grupoxusuario SET nivel = 'PESQUISADOR' WHERE nivel IS NULL;
UPDATE convite_grupo SET nivel = 'PESQUISADOR' WHERE nivel IS NULL;

-- DROP COLUMN remove também as foreign keys que dependem da coluna
ALTER TABLE grupoxusuario DROP COLUMN IF EXISTS role_id;
ALTER TABLE convite_grupo DROP COLUMN IF EXISTS role_id;

DROP TABLE IF EXISTS rolexpermissao;
DROP TABLE IF EXISTS role;
