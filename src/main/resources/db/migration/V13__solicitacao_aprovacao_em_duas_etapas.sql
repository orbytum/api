-- Solicitações: aprovação em duas etapas (Líder do grupo -> administrador).
-- Status 6 = AGUARDANDO_ADMINISTRADOR (status é inteiro, sem restrição de domínio).
ALTER TABLE material_emprestimo_solicitacao ADD COLUMN IF NOT EXISTS titulo VARCHAR(200);
ALTER TABLE material_emprestimo_solicitacao ADD COLUMN IF NOT EXISTS descricao TEXT;
ALTER TABLE material_emprestimo_solicitacao ADD COLUMN IF NOT EXISTS aprovador_lider_id BIGINT REFERENCES usuario (id);
ALTER TABLE material_emprestimo_solicitacao ADD COLUMN IF NOT EXISTS dth_aprovacao_lider TIMESTAMP;
ALTER TABLE material_emprestimo_solicitacao DROP CONSTRAINT IF EXISTS material_emprestimo_solicitacao_status_check;
ALTER TABLE material_emprestimo_solicitacao ADD CONSTRAINT material_emprestimo_solicitacao_status_check
    CHECK (status IN (1, 2, 3, 4, 5, 6));
