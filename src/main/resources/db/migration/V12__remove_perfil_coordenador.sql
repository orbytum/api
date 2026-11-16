-- Remoção do perfil Coordenador: os perfis passam a ser Líder do grupo, Líder de projeto,
-- Pesquisador e Colaborador. Coordenadores existentes viram Pesquisadores.
-- Espelha RemocaoCoordenadorInitializer, que aplica a mesma conversão enquanto o Flyway está desativado.
UPDATE grupoxusuario   SET nivel = 'PESQUISADOR' WHERE nivel = 'COORDENADOR';
UPDATE convite_grupo   SET nivel = 'PESQUISADOR' WHERE nivel = 'COORDENADOR';
UPDATE projetoxusuario SET nivel = 'PESQUISADOR' WHERE nivel = 'COORDENADOR';

ALTER TABLE grupoxusuario   DROP CONSTRAINT IF EXISTS grupoxusuario_nivel_check;
ALTER TABLE convite_grupo   DROP CONSTRAINT IF EXISTS convite_grupo_nivel_check;
ALTER TABLE projetoxusuario DROP CONSTRAINT IF EXISTS projetoxusuario_nivel_check;

ALTER TABLE grupoxusuario   ADD CONSTRAINT grupoxusuario_nivel_check   CHECK (nivel IN ('LIDER', 'PESQUISADOR', 'COLABORADOR'));
ALTER TABLE convite_grupo   ADD CONSTRAINT convite_grupo_nivel_check   CHECK (nivel IN ('LIDER', 'PESQUISADOR', 'COLABORADOR'));
ALTER TABLE projetoxusuario ADD CONSTRAINT projetoxusuario_nivel_check CHECK (nivel IN ('LIDER', 'PESQUISADOR', 'COLABORADOR'));
