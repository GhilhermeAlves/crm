-- V088__agent_profile_and_memory.sql
-- Arquitetura do agente: identidade/comportamento estruturados + memória própria.
--
-- A) agent_config ganha a configuração permanente separada em camadas
--    (Identidade: nome/descrição/persona; Comportamento: objetivo/tom/regras/
--    instruções). system_prompt continua existindo como PROMPT LEGADO: enquanto
--    a identidade e o comportamento estiverem vazios, o runtime usa o legado
--    exatamente como antes — nenhum agente existente muda de comportamento.
--    Nenhum parsing automático do prompt legado é feito aqui.
--    Flags de capacidade nascem DESLIGADAS (safe default, opt-in por empresa).
--
-- B) agent_memory: informações relevantes e duradouras sobre um contato
--    (preferência, fato, perfil, contextual). NÃO é histórico (mensagens ficam
--    em omnichannel_messages) e NÃO substitui dados transacionais do CRM
--    (agenda, status). Tenant-scoped com RLS FORCE (padrão V070/V077).

ALTER TABLE agent_config ADD COLUMN agent_name VARCHAR(120);
ALTER TABLE agent_config ADD COLUMN agent_description VARCHAR(500);
ALTER TABLE agent_config ADD COLUMN persona TEXT;
ALTER TABLE agent_config ADD COLUMN objective TEXT;
ALTER TABLE agent_config ADD COLUMN tone VARCHAR(500);
ALTER TABLE agent_config ADD COLUMN rules JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE agent_config ADD COLUMN instructions JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE agent_config ADD COLUMN memory_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE agent_config ADD COLUMN human_transfer_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- ===========================================================================
-- agent_memory
-- ===========================================================================
CREATE TABLE agent_memory (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    agent_config_id   UUID NOT NULL REFERENCES agent_config(id) ON DELETE CASCADE,
    contact_id        UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    memory_type       VARCHAR(20) NOT NULL,
    content           VARCHAR(500) NOT NULL,
    importance        SMALLINT NOT NULL DEFAULT 3 CHECK (importance BETWEEN 1 AND 5),
    source            VARCHAR(20) NOT NULL DEFAULT 'AGENT',
    source_message_id UUID,
    metadata          JSONB NOT NULL DEFAULT '{}'::jsonb,
    expires_at        TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_agent_memory_type CHECK (memory_type IN ('PREFERENCE', 'FACT', 'PROFILE', 'CONTEXTUAL')),
    CONSTRAINT chk_agent_memory_source CHECK (source IN ('AGENT', 'USER'))
);

-- Recuperação: memórias de um contato, mais importantes e mais recentes primeiro.
CREATE INDEX idx_agent_memory_contact_rank
    ON agent_memory (company_id, contact_id, importance DESC, updated_at DESC);

ALTER TABLE agent_memory ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_memory FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON agent_memory
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON agent_memory TO crm_app;
