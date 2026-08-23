-- Bootstrap para teste de integração RLS do módulo de Pipeline (Sprint 11)
-- Replica o estado-alvo pós-migração V017/V019/V020 para as tabelas de CRM
-- (companies, users, contacts, pipelines, stages, opportunities,
-- opportunity_history + policy de isolamento por tenant).
-- Não depende da cadeia V001-V038 (que tem peso/ordem desnecessários p/ IT).

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

CREATE TABLE IF NOT EXISTS pipelines (
    id              UUID PRIMARY KEY,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stages (
    id              UUID PRIMARY KEY,
    pipeline_id     UUID NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(7),
    "order"         INTEGER NOT NULL,
    probability     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opportunities (
    id                  UUID PRIMARY KEY,
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title               VARCHAR(200) NOT NULL,
    value               DECIMAL(15, 2) NOT NULL,
    contact_id          UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    pipeline_id         UUID NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    stage_id            UUID NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    assigned_to         UUID REFERENCES users(id) ON DELETE SET NULL,
    expected_close_date TIMESTAMP,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    won_at              TIMESTAMP,
    lost_at             TIMESTAMP,
    loss_reason         TEXT,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opportunity_history (
    id              UUID PRIMARY KEY,
    opportunity_id  UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    from_stage_id   UUID REFERENCES stages(id) ON DELETE SET NULL,
    to_stage_id     UUID NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    changed_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note            TEXT
);

-- ===========================================================================
-- RLS: pipelines, stages, opportunities, opportunity_history (isolamento por
-- tenant, FORCE — mesmo comportamento da V019/V020 em produção)
-- ===========================================================================
ALTER TABLE pipelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE pipelines FORCE ROW LEVEL SECURITY;
ALTER TABLE stages ENABLE ROW LEVEL SECURITY;
ALTER TABLE stages FORCE ROW LEVEL SECURITY;
ALTER TABLE opportunities ENABLE ROW LEVEL SECURITY;
ALTER TABLE opportunities FORCE ROW LEVEL SECURITY;
ALTER TABLE opportunity_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE opportunity_history FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_policy ON pipelines;
CREATE POLICY tenant_isolation_policy ON pipelines
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON stages;
CREATE POLICY tenant_isolation_policy ON stages
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON opportunities;
CREATE POLICY tenant_isolation_policy ON opportunities
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- opportunity_history não tem company_id; isola pela oportunidade-pai.
DROP POLICY IF EXISTS tenant_isolation_policy ON opportunity_history;
CREATE POLICY tenant_isolation_policy ON opportunity_history
    USING (opportunity_id IN (SELECT id FROM opportunities WHERE company_id = app.current_tenant_id()))
    WITH CHECK (opportunity_id IN (SELECT id FROM opportunities WHERE company_id = app.current_tenant_id()));
