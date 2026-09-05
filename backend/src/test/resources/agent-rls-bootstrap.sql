-- Bootstrap para teste de integração RLS do agente de IA (Sprint 2)
-- Replica o estado pós-migração V070 (agent_config + agent_auto_replies) com os
-- campos de geração da V071 (model/temperature/max_tokens) + companies base.
-- Segue o padrão de omnichannel-rls-bootstrap.sql — sem depender da cadeia V001-V071.

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

-- Conversa mínima para satisfazer a FK tenant-scoped de agent_auto_replies.
CREATE TABLE IF NOT EXISTS omnichannel_conversations (
    id             UUID PRIMARY KEY,
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    channel_id     UUID,
    external_phone VARCHAR(40),
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    last_message_at TIMESTAMP,
    unread_count   INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===========================================================================
-- V070: agent_config (+ colunas V071)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS agent_config (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    ai_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    allow_auto_reply  BOOLEAN NOT NULL DEFAULT FALSE,
    system_prompt     TEXT,
    model             VARCHAR(120),
    temperature       NUMERIC(4,3),
    max_tokens        INTEGER,
    cooldown_minutes  INT NOT NULL DEFAULT 60 CHECK (cooldown_minutes BETWEEN 0 AND 1440),
    max_chars         INT NOT NULL DEFAULT 1000 CHECK (max_chars BETWEEN 1 AND 5000),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE agent_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_config FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON agent_config;
CREATE POLICY tenant_isolation_policy ON agent_config
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- V070: agent_auto_replies (reserva idempotente + cooldown)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS agent_auto_replies (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id         UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id    UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    inbound_message_id UUID NOT NULL,
    replied_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_agent_auto_replies_company_inbound
    ON agent_auto_replies (company_id, inbound_message_id);
CREATE INDEX IF NOT EXISTS idx_agent_auto_replies_conversation_replied
    ON agent_auto_replies (company_id, conversation_id, replied_at DESC);

ALTER TABLE agent_auto_replies ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_auto_replies FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON agent_auto_replies;
CREATE POLICY tenant_isolation_policy ON agent_auto_replies
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());