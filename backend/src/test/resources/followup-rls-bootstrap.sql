-- Bootstrap para teste de integração RLS de FollowUp (Sprint 22)
-- Replica o estado pós-migração V073 (followups) sobre o núcleo omnichannel
-- (V044/V054) — sem depender da cadeia V001-V074.
-- Segue o padrão de omnichannel-rls-bootstrap.sql (Sprint 16).

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
-- V044: omnichannel_channels / conversations / messages (mesma estrutura real)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS omnichannel_channels (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    provider    VARCHAR(30) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    external_id VARCHAR(120),
    config      TEXT,
    secrets_ref VARCHAR(200),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_omnichannel_channels_company_external
    ON omnichannel_channels (company_id, external_id) WHERE external_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS omnichannel_conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    channel_id      UUID NOT NULL REFERENCES omnichannel_channels(id) ON DELETE CASCADE,
    contact_id      UUID,
    external_phone  VARCHAR(40) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handoff_mode    VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC',
    last_message_at TIMESTAMP,
    unread_count    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_omnichannel_conversations_channel_phone
    ON omnichannel_conversations (company_id, channel_id, external_phone);

-- V054: FK compostas com escopo de tenant (defesa em profundidade).
ALTER TABLE omnichannel_conversations DROP CONSTRAINT IF EXISTS uq_omnichannel_conversations_id_company;
ALTER TABLE omnichannel_conversations ADD CONSTRAINT uq_omnichannel_conversations_id_company UNIQUE (id, company_id);
ALTER TABLE omnichannel_channels DROP CONSTRAINT IF EXISTS uq_omnichannel_channels_id_company;
ALTER TABLE omnichannel_channels ADD CONSTRAINT uq_omnichannel_channels_id_company UNIQUE (id, company_id);

-- ===========================================================================
-- V073: followups (mesma estrutura real)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS followups (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id      UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    action_type          VARCHAR(20) NOT NULL DEFAULT 'SEND_MESSAGE',
    action_content       TEXT,
    execute_at           TIMESTAMP NOT NULL,
    attempts             INT NOT NULL DEFAULT 0,
    last_error           TEXT,
    result_text          TEXT,
    processing_started_at TIMESTAMP,
    processed_at         TIMESTAMP,
    cancelled_at         TIMESTAMP,
    cancelled_reason     VARCHAR(40),
    idempotency_key      UUID,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_followups_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_followups_action
        CHECK (action_type IN ('SEND_MESSAGE')),
    CONSTRAINT chk_followups_cancelled_reason
        CHECK (cancelled_reason IS NULL OR cancelled_reason IN ('USER', 'HUMAN_MODE', 'SUPERSEDED_BY_NEW_MESSAGE'))
);
ALTER TABLE followups DROP CONSTRAINT IF EXISTS fk_followups_conversation_tenant;
ALTER TABLE followups ADD CONSTRAINT fk_followups_conversation_tenant
    FOREIGN KEY (conversation_id, company_id)
    REFERENCES omnichannel_conversations (id, company_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_followups_company ON followups (company_id);
CREATE INDEX IF NOT EXISTS idx_followups_conversation_created ON followups (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_followups_due ON followups (execute_at) WHERE status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS uq_followups_company_idempotency
    ON followups (company_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE followups ENABLE ROW LEVEL SECURITY;
ALTER TABLE followups FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON followups;
CREATE POLICY tenant_isolation_policy ON followups
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE OR REPLACE FUNCTION app.followup_scheduler_candidates(p_limit INT)
RETURNS TABLE(followup_id UUID, company_id UUID)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS 'SELECT f.id, f.company_id
      FROM followups f
     WHERE (f.status = ''PENDING'' AND f.execute_at <= NOW())
        OR (f.status = ''PROCESSING'' AND f.processing_started_at < NOW() - INTERVAL ''15 minutes'')
     ORDER BY f.execute_at
     LIMIT p_limit';
REVOKE ALL ON FUNCTION app.followup_scheduler_candidates(INT) FROM PUBLIC;