-- V044__omnichannel_tables.sql
-- Sprint 16: Omnichannel WhatsApp — fundação.
--
-- Segue o padrão estabelecido (V041/V039): company_id NOT NULL FK para
-- companies, timestamps NOT NULL, índices por company_id e RLS FORCE +
-- policy tenant_isolation_policy usando app.current_tenant_id().
--
-- Grants CRUD para o role crm_app são aplicados automaticamente pelo loop
-- dinâmico da V034, portanto NÃO precisamos de GRANT aqui.

-- ===========================================================================
-- app.resolve_channel_company — SECURITY DEFINER.
-- Permite ao webhook (requisição SEM sessão de usuário autenticado) resolver a
-- empresa proprietária de um canal a partir do identificador externo, sem
-- quebrar o isolamento: roda como owner (bypassa RLS apenas para esta consulta
-- de mapeamento) e o restante da persistência continua sob RLS FORCE via GUC.
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.resolve_channel_company(p_external_id TEXT)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_company UUID;
BEGIN
    SELECT company_id INTO v_company
    FROM omnichannel_channels
    WHERE external_id = p_external_id
    LIMIT 1;
    RETURN v_company;
END $$;

REVOKE ALL ON FUNCTION app.resolve_channel_company(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.resolve_channel_company(TEXT) TO crm_app;

-- ===========================================================================
-- omnichannel_channels — canal de comunicação (ex.: WhatsApp) de uma Company.
-- Apenas metadados e referência a secret externo; o segredo em si NÃO é
-- armazenado aqui (ver docs/WHATSAPP.md).
-- ===========================================================================
CREATE TABLE omnichannel_channels (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,          -- WHATSAPP
    provider    VARCHAR(30) NOT NULL,          -- WHATSAPP_CLOUD_API | FAKE
    name        VARCHAR(120) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE | ERROR
    external_id VARCHAR(120),                  -- phone number id / número
    config      TEXT,                          -- JSON de metadados (NÃO contém secrets)
    secrets_ref VARCHAR(200),                  -- referência p/ secret externo
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_omnichannel_channels_company_external
    ON omnichannel_channels (company_id, external_id) WHERE external_id IS NOT NULL;
CREATE INDEX idx_omnichannel_channels_company ON omnichannel_channels (company_id);

ALTER TABLE omnichannel_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_channels FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON omnichannel_channels
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- omnichannel_conversations — conversa (thread) por canal + telefone externo.
-- ===========================================================================
CREATE TABLE omnichannel_conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    channel_id      UUID NOT NULL REFERENCES omnichannel_channels(id) ON DELETE CASCADE,
    contact_id      UUID,                      -- contato do CRM (matching)
    external_phone  VARCHAR(40) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN | CLOSED
    last_message_at TIMESTAMP,
    unread_count    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_omnichannel_conversations_channel_phone
    ON omnichannel_conversations (company_id, channel_id, external_phone);
CREATE INDEX idx_omnichannel_conversations_company ON omnichannel_conversations (company_id);
CREATE INDEX idx_omnichannel_conversations_company_last
    ON omnichannel_conversations (company_id, last_message_at DESC);
CREATE INDEX idx_omnichannel_conversations_contact ON omnichannel_conversations (contact_id);

ALTER TABLE omnichannel_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_conversations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON omnichannel_conversations
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- omnichannel_messages — mensagem. Duas chaves de idempotência:
--   * client_message_id: id gerado pelo CRM no ENVIO (retry seguro);
--   * external_message_id: id do provedor (wamid) para recebimento/status.
-- Status reflete o provedor: PENDING/SENT/DELIVERED/READ/FAILED.
-- ===========================================================================
CREATE TABLE omnichannel_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id     UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    channel_id          UUID NOT NULL REFERENCES omnichannel_channels(id) ON DELETE CASCADE,
    direction           VARCHAR(10) NOT NULL,  -- INBOUND | OUTBOUND
    sender_phone        VARCHAR(40),
    recipient_phone     VARCHAR(40),
    type                VARCHAR(20) NOT NULL DEFAULT 'TEXT',  -- TEXT
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

CREATE UNIQUE INDEX uq_omnichannel_messages_client
    ON omnichannel_messages (company_id, client_message_id);
-- Não-parcial de propósito: permite ON CONFLICT (company_id, external_message_id)
-- (PostgreSQL trata NULLs como distintos, então mensagens sem id externo não colidem).
CREATE UNIQUE INDEX uq_omnichannel_messages_external
    ON omnichannel_messages (company_id, external_message_id);
CREATE INDEX idx_omnichannel_messages_company ON omnichannel_messages (company_id);
CREATE INDEX idx_omnichannel_messages_conversation_created
    ON omnichannel_messages (conversation_id, created_at);

ALTER TABLE omnichannel_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_messages FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON omnichannel_messages
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
