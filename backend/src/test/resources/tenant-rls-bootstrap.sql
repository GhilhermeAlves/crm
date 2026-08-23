-- Bootstrap para teste de integração RLS (Sprint 5)
-- Replica o estado-alvo pós-migração V019/V020 (schema + RLS)
-- Não depende da cadeia V001-V020 (que contém bugs pré-existentes V008/V017)

CREATE SCHEMA IF NOT EXISTS app;

CREATE OR REPLACE FUNCTION app.current_tenant_id()
RETURNS UUID
LANGUAGE SQL
STABLE
AS 'SELECT NULLIF(current_setting(''app.current_company_id'', TRUE), '''')::UUID';

CREATE TABLE IF NOT EXISTS companies (
    id                     UUID PRIMARY KEY,
    legal_name             VARCHAR(255) NOT NULL,
    trading_name           VARCHAR(255) NOT NULL,
    cnpj                   VARCHAR(20)  NOT NULL UNIQUE,
    state_registration     VARCHAR(50),
    municipal_registration VARCHAR(50),
    email                  VARCHAR(255) NOT NULL UNIQUE,
    phone                  VARCHAR(30)  NOT NULL,
    website                VARCHAR(255),
    address_zip_code       VARCHAR(20)  NOT NULL,
    address_street         VARCHAR(255) NOT NULL,
    address_number         VARCHAR(20)  NOT NULL,
    address_complement     VARCHAR(255),
    address_neighborhood   VARCHAR(150) NOT NULL,
    address_city           VARCHAR(100) NOT NULL,
    address_state          VARCHAR(2)   NOT NULL,
    address_country        VARCHAR(100) NOT NULL,
    plan                   VARCHAR(50)  NOT NULL,
    status                 VARCHAR(50)  NOT NULL,
    max_users              INTEGER      NOT NULL,
    max_storage_mb         INTEGER      NOT NULL,
    logo_url               VARCHAR(500),
    notes                  TEXT,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    company_id     UUID NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMP,
    first_name     VARCHAR(255) NOT NULL DEFAULT '',
    last_name      VARCHAR(255),
    phone          VARCHAR(30),
    department     VARCHAR(100),
    job_title      VARCHAR(100),
    avatar_url     VARCHAR(500),
    status         VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    language       VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    timezone       VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    notes          TEXT,
    last_login_at  TIMESTAMP,
    invite_token   VARCHAR(255),
    invited_at     TIMESTAMP,
    invited_by     UUID,
    keycloak_sub   VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL,
    company_id  UUID NOT NULL,
    description VARCHAR(255),
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    company_id  UUID NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    family      VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token      VARCHAR(500) NOT NULL UNIQUE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL,
    user_id        UUID,
    user_name      VARCHAR(255),
    user_email     VARCHAR(255),
    action         VARCHAR(50) NOT NULL,
    module         VARCHAR(50) NOT NULL,
    entity_name    VARCHAR(100),
    entity_id      VARCHAR(100),
    description    TEXT,
    old_values     JSONB,
    new_values     JSONB,
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    request_method VARCHAR(10),
    request_uri    TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    success        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- RLS: 6 tabelas exercitadas no teste (subconjunto das 12 do Sprint 5)
-- ===========================================================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON users USING (company_id = app.current_tenant_id());

ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON roles USING (company_id = app.current_tenant_id());

ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON user_roles USING (company_id = app.current_tenant_id());

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON audit_logs USING (company_id = app.current_tenant_id());

ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON refresh_tokens
    USING (EXISTS (SELECT 1 FROM users u WHERE u.id = refresh_tokens.user_id AND u.company_id = app.current_tenant_id()));

ALTER TABLE password_reset_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_tokens FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON password_reset_tokens
    USING (EXISTS (SELECT 1 FROM users u WHERE u.id = password_reset_tokens.user_id AND u.company_id = app.current_tenant_id()));
