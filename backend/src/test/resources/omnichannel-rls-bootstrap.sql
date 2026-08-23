-- Bootstrap para teste de integração RLS de Omnichannel (Sprint 16)
-- Replica o estado pós-migração V044 (channels/conversations/messages) +
-- company/contact base — sem depender da cadeia V001-V044.
-- Segue o padrão de activity-task-rls-bootstrap.sql (Sprint 12).

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
    last_message_at TIMESTAMP,
    unread_count    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_omnichannel_conversations_channel_phone
    ON omnichannel_conversations (company_id, channel_id, external_phone);

CREATE TABLE IF NOT EXISTS omnichannel_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id     UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    channel_id          UUID NOT NULL REFERENCES omnichannel_channels(id) ON DELETE CASCADE,
    direction           VARCHAR(10) NOT NULL,
    sender_phone        VARCHAR(40),
    recipient_phone     VARCHAR(40),
    type                VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    body                TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_message_id VARCHAR(120),
    client_message_id   UUID NOT NULL,
    provider_error      TEXT,
    sent_at             TIMESTAMP,
    received_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_omnichannel_messages_client
    ON omnichannel_messages (company_id, client_message_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_omnichannel_messages_external
    ON omnichannel_messages (company_id, external_message_id);

-- V054: FKs compostas com escopo de tenant (defesa em profundidade).
ALTER TABLE omnichannel_conversations DROP CONSTRAINT IF EXISTS uq_omnichannel_conversations_id_company;
ALTER TABLE omnichannel_conversations ADD CONSTRAINT uq_omnichannel_conversations_id_company UNIQUE (id, company_id);
ALTER TABLE omnichannel_channels DROP CONSTRAINT IF EXISTS uq_omnichannel_channels_id_company;
ALTER TABLE omnichannel_channels ADD CONSTRAINT uq_omnichannel_channels_id_company UNIQUE (id, company_id);
ALTER TABLE omnichannel_messages DROP CONSTRAINT IF EXISTS fk_omnichannel_messages_conversation_tenant;
ALTER TABLE omnichannel_messages ADD CONSTRAINT fk_omnichannel_messages_conversation_tenant
    FOREIGN KEY (conversation_id, company_id)
    REFERENCES omnichannel_conversations (id, company_id) ON DELETE CASCADE;
ALTER TABLE omnichannel_messages DROP CONSTRAINT IF EXISTS fk_omnichannel_messages_channel_tenant;
ALTER TABLE omnichannel_messages ADD CONSTRAINT fk_omnichannel_messages_channel_tenant
    FOREIGN KEY (channel_id, company_id)
    REFERENCES omnichannel_channels (id, company_id) ON DELETE CASCADE;

ALTER TABLE omnichannel_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_channels FORCE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_conversations FORCE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_messages FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_policy ON omnichannel_channels;
CREATE POLICY tenant_isolation_policy ON omnichannel_channels
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON omnichannel_conversations;
CREATE POLICY tenant_isolation_policy ON omnichannel_conversations
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_policy ON omnichannel_messages;
CREATE POLICY tenant_isolation_policy ON omnichannel_messages
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());