-- V156__report_exports_canceled.sql
-- Cancelamento pelo autor: novo status terminal CANCELED. RLS/GRANT da tabela
-- não mudam (V096).

ALTER TABLE report_exports DROP CONSTRAINT IF EXISTS ck_report_exports_status;
ALTER TABLE report_exports ADD CONSTRAINT ck_report_exports_status
    CHECK (status IN ('PENDING', 'READY', 'FAILED', 'EXPIRED', 'CANCELED'));

-- Limite de pedidos e varredura de travados: por empresa/usuário e por criação.
CREATE INDEX IF NOT EXISTS idx_report_exports_user_created
    ON report_exports (company_id, requested_by, created_at);
CREATE INDEX IF NOT EXISTS idx_report_exports_pending_created
    ON report_exports (company_id, created_at) WHERE status = 'PENDING';
