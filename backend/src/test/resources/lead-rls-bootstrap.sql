-- Bootstrap para teste de integração RLS do módulo de Leads (Sprint 10)
-- Replica o estado-alvo pós-migração V016/V021 para as tabelas de CRM
-- (companies, users, contacts, leads + policy de isolamento por tenant).
-- Não depende da cadeia V001-V037 (que tem peso/ordem desnecessários p/ IT).

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

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    first_name    VARCHAR(255) NOT NULL DEFAULT '',
    company_id    UUID NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
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

CREATE TABLE IF NOT EXISTS leads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    score           INTEGER NOT NULL DEFAULT 0,
    classification  VARCHAR(20),
    source          VARCHAR(20) NOT NULL,
    campaign_id     UUID,
    assigned_to     UUID REFERENCES users(id) ON DELETE SET NULL,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_leads_contact_company ON leads (contact_id, company_id);

-- ===========================================================================
-- RLS: contacts e leads (isolamento por tenant, FORCE)
-- ===========================================================================
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;
ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE leads FORCE ROW LEVEL SECURITY;

-- contacts: company_id direto
DROP POLICY IF EXISTS tenant_isolation_policy ON contacts;
CREATE POLICY tenant_isolation_policy ON contacts
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- leads: company_id direto
DROP POLICY IF EXISTS tenant_isolation_policy ON leads;
CREATE POLICY tenant_isolation_policy ON leads
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());