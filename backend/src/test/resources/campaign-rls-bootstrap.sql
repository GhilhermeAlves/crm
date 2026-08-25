-- Bootstrap RLS para ITs de Campanha (Sprint 17).
-- Executar APÓS tenant-rls-bootstrap.sql. Replica o estado pós V055–V059
-- (tabelas de campanha + templates + RLS FORCE).

CREATE TABLE IF NOT EXISTS message_templates (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                 VARCHAR(120) NOT NULL,
    channel_type         VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
    subject              VARCHAR(200),
    body                 TEXT NOT NULL,
    variables            TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version              INT NOT NULL DEFAULT 1,
    external_template_id VARCHAR(120),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE message_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_templates FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON message_templates;
CREATE POLICY tenant_isolation_policy ON message_templates
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE TABLE IF NOT EXISTS campaigns (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                 VARCHAR(120) NOT NULL,
    description          TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    audience_type        VARCHAR(20) NOT NULL DEFAULT 'CONTACTS',
    audience_criteria    TEXT,
    estimated_recipients INT NOT NULL DEFAULT 0,
    scheduled_at         TIMESTAMP,
    timezone             VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    started_at           TIMESTAMP,
    completed_at         TIMESTAMP,
    created_by           UUID,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaigns FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON campaigns;
CREATE POLICY tenant_isolation_policy ON campaigns
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

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
ALTER TABLE omnichannel_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_channels FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON omnichannel_channels;
CREATE POLICY tenant_isolation_policy ON omnichannel_channels
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE TABLE IF NOT EXISTS campaign_channels (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    campaign_id         UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    channel_type        VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
    provider_channel_id UUID NOT NULL REFERENCES omnichannel_channels(id) ON DELETE CASCADE,
    template_id         UUID NOT NULL REFERENCES message_templates(id),
    template_version    INT NOT NULL DEFAULT 1,
    config              TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE campaign_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_channels FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON campaign_channels;
CREATE POLICY tenant_isolation_policy ON campaign_channels
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE TABLE IF NOT EXISTS contacts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    email       VARCHAR(255),
    phone       VARCHAR(20),
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP
);
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON contacts;
CREATE POLICY tenant_isolation_policy ON contacts
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE TABLE IF NOT EXISTS leads (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id  UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL DEFAULT 'NEW',
    score       INT NOT NULL DEFAULT 0,
    source      VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE leads FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON leads;
CREATE POLICY tenant_isolation_policy ON leads
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE TABLE IF NOT EXISTS campaign_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    campaign_id       UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    template_snapshot TEXT,
    total_recipients  INT NOT NULL DEFAULT 0,
    processed_count   INT NOT NULL DEFAULT 0,
    failed_count      INT NOT NULL DEFAULT 0,
    cursor_offset     INT NOT NULL DEFAULT 0,
    started_at        TIMESTAMP,
    finished_at       TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE campaign_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_executions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON campaign_executions;
CREATE POLICY tenant_isolation_policy ON campaign_executions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

CREATE TABLE IF NOT EXISTS campaign_message_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    execution_id        UUID NOT NULL REFERENCES campaign_executions(id) ON DELETE CASCADE,
    campaign_id         UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    recipient_id        UUID NOT NULL,
    recipient_type      VARCHAR(10) NOT NULL DEFAULT 'CONTACT',
    recipient_phone     VARCHAR(40),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts            INT NOT NULL DEFAULT 0,
    error_reason        TEXT,
    provider_message_id VARCHAR(120),
    occurred_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_campaign_message_events_execution_recipient
    ON campaign_message_events (execution_id, recipient_id);
ALTER TABLE campaign_message_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_message_events FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON campaign_message_events;
CREATE POLICY tenant_isolation_policy ON campaign_message_events
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
