-- Bootstrap para teste de integração RLS de Workflow (Sprint 14)
-- Replica o estado pós-migração V041 para workflows/conditions/actions/executions
-- (mais company/contact) — sem depender da cadeia V001-V042.
-- Segue exatamente o padrão de activity-task-rls-bootstrap.sql (Sprint 12).

CREATE SCHEMA IF NOT EXISTS app;

CREATE OR REPLACE FUNCTION app.current_tenant_id()
RETURNS UUID
LANGUAGE SQL
STABLE
AS 'SELECT NULLIF(current_setting(''app.current_company_id'', TRUE), '''')::UUID';

CREATE TABLE IF NOT EXISTS companies (
    id             UUID PRIMARY KEY,
    legal_name     VARCHAR(255) NOT NULL,
    trading_name   VARCHAR(255) NOT NULL,
    cnpj           VARCHAR(20)  NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone          VARCHAR(30)  NOT NULL,
    address_zip_code       VARCHAR(20) NOT NULL,
    address_street         VARCHAR(255) NOT NULL,
    address_number         VARCHAR(20) NOT NULL,
    address_neighborhood   VARCHAR(150) NOT NULL,
    address_city           VARCHAR(100) NOT NULL,
    address_state          VARCHAR(2) NOT NULL,
    address_country        VARCHAR(100) NOT NULL,
    plan                   VARCHAR(50) NOT NULL,
    status                 VARCHAR(50) NOT NULL,
    max_users              INTEGER NOT NULL,
    max_storage_mb         INTEGER NOT NULL,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    email           VARCHAR(255),
    phone           VARCHAR(20),
    company_name    VARCHAR(200),
    notes           TEXT,
    avatar_url      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

-- ===========================================================================
-- V041: workflows (mesma estrutura da migration real)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS workflows (
    id              UUID PRIMARY KEY,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    trigger         VARCHAR(40) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workflow_conditions (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    field       VARCHAR(60) NOT NULL,
    operator    VARCHAR(25) NOT NULL,
    value       TEXT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS workflow_actions (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    action_type VARCHAR(30) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    config      TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_executions (
    id                  UUID PRIMARY KEY,
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id         UUID REFERENCES workflows(id) ON DELETE SET NULL,
    workflow_action_id  UUID NOT NULL,
    event_id            UUID NOT NULL,
    event_type          VARCHAR(40) NOT NULL,
    entity_id           UUID,
    action_type         VARCHAR(30) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    result_text         TEXT,
    error_message       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE workflow_executions ADD CONSTRAINT uq_workflow_executions_dedup
    UNIQUE (company_id, workflow_action_id, event_id);

ALTER TABLE workflows ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflows FORCE ROW LEVEL SECURITY;
ALTER TABLE workflow_conditions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_conditions FORCE ROW LEVEL SECURITY;
ALTER TABLE workflow_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_actions FORCE ROW LEVEL SECURITY;
ALTER TABLE workflow_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_executions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_policy ON workflows;
CREATE POLICY tenant_isolation_policy ON workflows
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON workflow_conditions;
CREATE POLICY tenant_isolation_policy ON workflow_conditions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON workflow_actions;
CREATE POLICY tenant_isolation_policy ON workflow_actions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON workflow_executions;
CREATE POLICY tenant_isolation_policy ON workflow_executions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());