-- A coluna atividade.status passou a INTEGER (AtividadeStatus: 1..5).
-- Bancos que criando a coluna como SMALLINT ficaram com a restricao
-- "status >= 0 AND status <= 3", que impede CONCLUIDA(4) e ENCERRADA(5).
ALTER TABLE atividade DROP CONSTRAINT IF EXISTS atividade_status_check;
