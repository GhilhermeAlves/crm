-- V096__report_exports.sql
-- Exportações de relatórios (menu Relatórios → submenu de impressão/exportação).
-- Cada pedido de PDF/XLSX vira um registro PENDING; um job assíncrono gera o
-- arquivo, guarda em storage_objects e avisa o usuário no sino (notificação com
-- link de download). O registro serve de trilha e de controle do download (só o
-- próprio solicitante baixa).
--
-- Mesmo padrão da V093: company_id desnormalizado + RLS FORCE direta.

-- ===========================================================================
-- report_exports
-- ===========================================================================
CREATE TABLE report_exports (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    requested_by      UUID NOT NULL,
    type              VARCHAR(40) NOT NULL,
    format            VARCHAR(5)  NOT NULL,
    period_from       DATE NOT NULL,
    period_to         DATE NOT NULL,
    status            VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    file_name         VARCHAR(200),
    storage_object_id UUID,
    error_message     VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at       TIMESTAMP,
    CONSTRAINT ck_report_exports_type   CHECK (type IN ('MONTHLY_DRE', 'CASH_FLOW', 'TRANSACTIONS', 'AGENDA')),
    CONSTRAINT ck_report_exports_format CHECK (format IN ('PDF', 'XLSX')),
    CONSTRAINT ck_report_exports_status CHECK (status IN ('PENDING', 'READY', 'FAILED')),
    CONSTRAINT ck_report_exports_period CHECK (period_to >= period_from)
);

CREATE INDEX idx_report_exports_company_user ON report_exports (company_id, requested_by, created_at DESC);

-- ===========================================================================
-- RLS FORCE (isolamento por tenant)
-- ===========================================================================
ALTER TABLE report_exports ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_exports FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON report_exports
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants (tabela é acessada pelo role de aplicação crm_app)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON report_exports TO crm_app;
