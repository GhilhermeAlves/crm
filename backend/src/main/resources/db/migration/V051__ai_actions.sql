-- V051__ai_actions.sql
-- AI-05 (Write Tools): proposta de acao de escrita do assistente de IA.
--
-- Segue o padrao das tabelas tenant-scoped (V050): company_id NOT NULL FK para
-- companies, RLS FORCE + policy tenant_isolation_policy usando
-- app.current_tenant_id(). A acao e vinculada a uma conversa da MESMA empresa
-- (conversation_id FK para ai_conversations), garantindo isolamento via RLS.
-- A posse (user_id = quem propoe e unico autorizado a confirmar/cancelar) e
-- validada no dominio/service - defense-in-depth alem do RLS.
--
-- A tabela persiste a PROPOSTA (estado PROPOSED) com os parametros tipados em
-- JSONB. A confirmacao executa os mesmos parametros - nunca reexecuta a partir
-- de entrada do LLM. version viabiliza locking otimista; o confirm tambem usa
-- SELECT FOR UPDATE para tornar a transicao PROPOSED -> terminal atomica.

CREATE TABLE ai_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    tool            VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    description     TEXT,
    parameters      JSONB NOT NULL DEFAULT '{}'::jsonb,
    status          VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
    result          JSONB,
    error_message   TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_actions_company_id      ON ai_actions (company_id);
CREATE INDEX idx_ai_actions_user_id         ON ai_actions (user_id);
CREATE INDEX idx_ai_actions_conversation_id ON ai_actions (conversation_id);
CREATE INDEX idx_ai_actions_status          ON ai_actions (status);

ALTER TABLE ai_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_actions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON ai_actions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- Grants idempotentes para crm_app (mesmo padrao da V046/V050).
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_actions TO crm_app;