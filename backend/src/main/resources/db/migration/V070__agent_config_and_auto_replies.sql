-- V070__agent_config_and_auto_replies.sql
-- Portabilidade Q7 -> CRM (Sprint 1): agente de IA autonomo por empresa.
--
-- Duas tabelas tenant-scoped:
--   * agent_config          -> regras do agente (prompt, AI ativo, auto-resposta,
--                              limites de seguranca). AUSENCIA de linha = sem
--                              auto-resposta (safe defaults em codigo).
--   * agent_auto_replies    -> reserva idempotente de 1 resposta por mensagem
--                              ENTRADA e registro de cooldown (janela de 60 min).
--
-- Segue o padrao V044/V050: company_id NOT NULL FK para companies, timestamps,
-- RLS FORCE + policy tenant_isolation_policy usando app.current_tenant_id().
-- GRANT CRUD explicito e idempotente para crm_app (padrao V050) porque o loop
-- dinamico da V046 roda apenas no momento daquela migracao.

-- ===========================================================================
-- agent_config
-- ===========================================================================
CREATE TABLE agent_config (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    ai_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    allow_auto_reply  BOOLEAN NOT NULL DEFAULT FALSE,
    system_prompt     TEXT,
    cooldown_minutes  INT NOT NULL DEFAULT 60 CHECK (cooldown_minutes BETWEEN 0 AND 1440),
    max_chars         INT NOT NULL DEFAULT 1000 CHECK (max_chars BETWEEN 1 AND 5000),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE agent_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_config FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON agent_config
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- agent_auto_replies
-- ===========================================================================
CREATE TABLE agent_auto_replies (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id         UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id    UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    inbound_message_id UUID NOT NULL,
    replied_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Idempotencia: no maximo 1 auto-resposta por mensagem ENTRADA (mesmo quando a
-- mesma mensagem e reprocessada ou repassada ao listener).
CREATE UNIQUE INDEX uq_agent_auto_replies_company_inbound
    ON agent_auto_replies (company_id, inbound_message_id);
CREATE INDEX idx_agent_auto_replies_conversation_replied
    ON agent_auto_replies (company_id, conversation_id, replied_at DESC);

ALTER TABLE agent_auto_replies ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_auto_replies FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON agent_auto_replies
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants idempotentes para crm_app (mesmo padrao da V050)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON agent_config TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON agent_auto_replies TO crm_app;