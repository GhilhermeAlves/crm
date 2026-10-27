-- V113__budget_titles.sql
-- Títulos de orçamento salvos por empresa: o seletor de título do orçamento
-- oferece estes nomes e permite cadastrar novos para lançamentos futuros.

CREATE TABLE budget_titles (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    created_by  UUID NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_budget_titles_company_title ON budget_titles (company_id, lower(title));

ALTER TABLE budget_titles ENABLE ROW LEVEL SECURITY;
ALTER TABLE budget_titles FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON budget_titles
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON budget_titles TO crm_app;
