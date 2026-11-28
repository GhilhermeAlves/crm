-- V155__report_exports_expired.sql
-- Retenção de 7 dias das exportações de relatório: o job diário apaga o arquivo
-- do storage e marca o registro como EXPIRED (fica como histórico).
-- RLS/GRANT da tabela não mudam (V096); o job percorre as empresas via
-- app.all_company_ids() e opera dentro do tenant de cada uma.

ALTER TABLE report_exports DROP CONSTRAINT IF EXISTS ck_report_exports_status;
ALTER TABLE report_exports ADD CONSTRAINT ck_report_exports_status
    CHECK (status IN ('PENDING', 'READY', 'FAILED', 'EXPIRED'));

-- Varredura do expurgo: READY por empresa ordenado por finished_at.
CREATE INDEX IF NOT EXISTS idx_report_exports_retention
    ON report_exports (company_id, status, finished_at);
