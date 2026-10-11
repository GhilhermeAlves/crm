-- V094__report_suggestions.sql
-- Sugestões de relatório enviadas pelo diálogo "Sugerir um relatório" da tela
-- Relatórios da clínica (hub). Cada linha guarda os temas escolhidos pelo
-- usuário (categorias) e uma observação opcional. É feedback interno do
-- produto, não um relatório gerado.
--
-- Como a V092/V093, cada registro carrega company_id de forma desnormalizada
-- para uma policy de RLS FORCE direta (company_id = app.current_tenant_id()).
--
-- Sem novas permissões: qualquer membro da empresa pode sugerir, então o
-- endpoint exige apenas autenticação (isAuthenticated), não uma authority.

-- ===========================================================================
-- report_suggestions
-- ===========================================================================
CREATE TABLE report_suggestions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    categories  TEXT[] NOT NULL,
    notes       TEXT,
    created_by  UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_suggestions_company_created
    ON report_suggestions (company_id, created_at DESC);

-- ===========================================================================
-- RLS FORCE (isolamento por tenant)
-- ===========================================================================
ALTER TABLE report_suggestions ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_suggestions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON report_suggestions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants (tabela é acessada pelo role de aplicação crm_app)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON report_suggestions TO crm_app;
