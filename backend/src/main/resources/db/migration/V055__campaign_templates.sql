-- V055__campaign_templates.sql
-- Sprint 17: Campanhas — templates de mensagem por canal.
-- Segue o padrão estabelecido (V044/V039): company_id NOT NULL FK para
-- companies ON DELETE CASCADE, timestamps NOT NULL, índices por company_id e
-- RLS FORCE + policy tenant_isolation_policy usando app.current_tenant_id().
-- Grants CRUD para o role crm_app são aplicados pelo loop dinâmico da V034.

CREATE TABLE message_templates (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                 VARCHAR(120) NOT NULL,
    channel_type         VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',  -- WHATSAPP | EMAIL
    subject              VARCHAR(200),
    body                 TEXT NOT NULL,
    variables            TEXT,                                     -- JSON array com nomes de variáveis {{var}}
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',    -- ACTIVE | ARCHIVED
    version              INT NOT NULL DEFAULT 1,
    external_template_id VARCHAR(120),                             -- template aprovado no provider (Meta), se houver
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_message_templates_company ON message_templates (company_id);
CREATE INDEX idx_message_templates_company_channel ON message_templates (company_id, channel_type);

ALTER TABLE message_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_templates FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON message_templates
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
