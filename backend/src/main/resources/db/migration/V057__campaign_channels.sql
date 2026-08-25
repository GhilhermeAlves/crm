-- V057__campaign_channels.sql
-- Sprint 17: canal da campanha — desacopla Campaign do provider.
-- provider_channel_id referencia um canal Omnichannel ativo (V044).
-- template_version congela a versão do template usada pela campanha.

CREATE TABLE campaign_channels (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    campaign_id         UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    channel_type        VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',  -- WHATSAPP | EMAIL
    provider_channel_id UUID NOT NULL REFERENCES omnichannel_channels(id) ON DELETE CASCADE,
    template_id         UUID NOT NULL REFERENCES message_templates(id),
    template_version    INT NOT NULL DEFAULT 1,
    config              TEXT,                                     -- JSON de metadados específicos do canal
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Uma campanha tem um canal primário na Sprint 17 (multicanal = FUTURE).
CREATE UNIQUE INDEX uq_campaign_channels_campaign ON campaign_channels (campaign_id);
CREATE INDEX idx_campaign_channels_company ON campaign_channels (company_id);

ALTER TABLE campaign_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_channels FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON campaign_channels
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
