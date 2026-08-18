-- V050__ai_chat_tables.sql
-- AI-01 (Foundation): persistência do assistente de IA — conversas e mensagens.
--
-- Segue o padrão das tabelas tenant-scoped (V015/V017/V039): company_id NOT NULL
-- FK para companies, timestamps NOT NULL, RLS FORCE + policy
-- tenant_isolation_policy usando app.current_tenant_id(). As mensagens são
-- vinculadas a uma conversa da MESMA empresa (empresa = tenant), o que garante
-- isolamento via RLS mesmo na tabela de mensagens.
--
-- Os grants CRUD para crm_app são aplicados pelo loop dinâmico da V046, mas
-- para garantir na criação desta tabela executamos o mesmo GRANT idempotente.

-- ===========================================================================
-- AI Conversations (chat do assistente)
-- ===========================================================================
CREATE TABLE ai_conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    screen          VARCHAR(50),
    record_id       UUID,
    title           VARCHAR(200),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversations_company_id ON ai_conversations (company_id);
CREATE INDEX idx_ai_conversations_user_id    ON ai_conversations (user_id);
CREATE INDEX idx_ai_conversations_record_id  ON ai_conversations (record_id);
CREATE INDEX idx_ai_conversations_created_at ON ai_conversations (created_at DESC);

ALTER TABLE ai_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_conversations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON ai_conversations
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- AI Messages (mensagens do assistente)
-- ===========================================================================
CREATE TABLE ai_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_messages_company_id      ON ai_messages (company_id);
CREATE INDEX idx_ai_messages_conversation_id ON ai_messages (conversation_id);
CREATE INDEX idx_ai_messages_created_at      ON ai_messages (created_at ASC);

ALTER TABLE ai_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_messages FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON ai_messages
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants idempotentes para crm_app (mesmo padrão da V046)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_conversations TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_messages TO crm_app;

-- ===========================================================================
-- Permissão do assistente de IA (mesmo padrão da V045/V049)
-- O vínculo papel -> permissão é aplicado no startup pelo RoleSeedService.
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('ai:chat', 'Chat com o assistente de IA', 'ai', 'assistant', 'chat')
ON CONFLICT (name) DO NOTHING;
