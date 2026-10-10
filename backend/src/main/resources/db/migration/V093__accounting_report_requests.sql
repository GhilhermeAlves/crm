-- V093__accounting_report_requests.sql
-- Registro das solicitações de relatórios contábeis (tela "Gerar relatórios
-- contábeis" da Gestão financeira). Não é o arquivo gerado: serve de auditoria
-- e controla a regra de negócio de 1 relatório por tipo+formato+mês por empresa
-- (o reenvio apenas incrementa generated_count e regera o arquivo).
--
-- Assim como a V092, cada registro carrega company_id de forma desnormalizada
-- para uma policy de RLS FORCE direta (company_id = app.current_tenant_id()).
--
-- Sem novas permissões: a leitura usa finance:read e a gravação é feita pelo
-- próprio módulo financeiro (mesmas permissões finance:* da V092).

-- ===========================================================================
-- accounting_report_requests
-- ===========================================================================
CREATE TABLE accounting_report_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL,
    format          VARCHAR(5) NOT NULL,
    month           CHAR(7) NOT NULL,
    requested_by    UUID,
    requested_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_count INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_accounting_report_requests UNIQUE (company_id, type, format, month)
);

-- ===========================================================================
-- RLS FORCE (isolamento por tenant)
-- ===========================================================================
ALTER TABLE accounting_report_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounting_report_requests FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON accounting_report_requests
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants (tabela é acessada pelo role de aplicação crm_app)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON accounting_report_requests TO crm_app;