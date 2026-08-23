-- Bootstrap para teste de integração RLS de Activities + Tasks (Sprint 12)
-- Replica o estado pós-migração V039 para activities/tasks (mais company/contact
-- — sem depender da cadeia V001-V040, que tem peso desnecessário para o IT).
-- Segue exatamente o padrão de pipeline-rls-bootstrap.sql (Sprint 11) +
-- LeadIsolationIT.

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
-- V039: activities + tasks (mesma estrutura que a migration real)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS activities (
    id              UUID PRIMARY KEY,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES contacts(id)   ON DELETE SET NULL,
    opportunity_id  UUID,
    type            VARCHAR(30) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    description     TEXT,
    activity_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE activities ADD CONSTRAINT chk_activities_type CHECK (type IN (
    'CALL', 'MEETING', 'EMAIL', 'MESSAGE', 'NOTE', 'PROPOSAL', 'FOLLOW_UP', 'OTHER'
));

CREATE TABLE IF NOT EXISTS tasks (
    id              UUID PRIMARY KEY,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES contacts(id)   ON DELETE SET NULL,
    opportunity_id  UUID,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    assignee_id     UUID,
    due_at          TIMESTAMP,
    priority        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tasks ADD CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

ALTER TABLE activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE activities FORCE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_policy ON activities;
CREATE POLICY tenant_isolation_policy ON activities
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON tasks;
CREATE POLICY tenant_isolation_policy ON tasks
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());