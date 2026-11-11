-- Perfis (COLABORADOR), aprovação de atividades, rótulos de editais/notícias e escopo de lembretes.

-- Lembrete vinculado a projeto (nulo = grupo inteiro)
ALTER TABLE evento ADD COLUMN IF NOT EXISTS projeto_id BIGINT;

ALTER TABLE evento DROP CONSTRAINT IF EXISTS fk_evento_on_projeto;
ALTER TABLE evento ADD CONSTRAINT fk_evento_on_projeto
    FOREIGN KEY (projeto_id) REFERENCES projeto (id);

CREATE INDEX IF NOT EXISTS idx_evento_projeto ON evento (projeto_id);

-- Rótulos de edital
CREATE TABLE IF NOT EXISTS edital_x_projeto
(
    edital_id  BIGINT NOT NULL,
    projeto_id BIGINT NOT NULL,
    CONSTRAINT pk_edital_x_projeto PRIMARY KEY (edital_id, projeto_id),
    CONSTRAINT fk_edital_x_projeto_on_edital FOREIGN KEY (edital_id) REFERENCES edital (id) ON DELETE CASCADE,
    CONSTRAINT fk_edital_x_projeto_on_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id) ON DELETE CASCADE
);

-- Rótulos de notícia
CREATE TABLE IF NOT EXISTS noticia_x_projeto
(
    noticia_id BIGINT NOT NULL,
    projeto_id BIGINT NOT NULL,
    CONSTRAINT pk_noticia_x_projeto PRIMARY KEY (noticia_id, projeto_id),
    CONSTRAINT fk_noticia_x_projeto_on_noticia FOREIGN KEY (noticia_id) REFERENCES noticia (id) ON DELETE CASCADE,
    CONSTRAINT fk_noticia_x_projeto_on_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id) ON DELETE CASCADE
);

-- Status de atividade: remove check legado que poderia bloquear 0 (AGUARDANDO_APROVACAO) e 6 (REJEITADA)
ALTER TABLE atividade DROP CONSTRAINT IF EXISTS atividade_status_check;

-- Motivo de rejeição de atividade
ALTER TABLE atividade ADD COLUMN IF NOT EXISTS motivo_rejeicao VARCHAR(255);

-- NivelMembro.COLABORADOR usa as colunas VARCHAR(20) já existentes em grupoxusuario/projetoxusuario.
