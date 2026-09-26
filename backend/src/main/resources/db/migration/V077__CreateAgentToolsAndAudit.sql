-- V077__CreateAgentToolsAndAudit.sql
-- WhatsApp AI Agent — Fase 1: ferramentas do agente, permissões por papel e
-- trilha de auditoria das ações executadas pelo agente.
--
-- Todas as tabelas são recursos de tenant com RLS FORCE (padrão V076).
-- Safe default: ferramentas nascem desabilitadas (enabled = FALSE) e
-- permissões nascem negadas (allowed = FALSE) — opt-in por empresa.

-- ===========================================================================
-- agent_tool — ferramentas disponíveis para o agente, por empresa.
-- ===========================================================================
CREATE TABLE agent_tool (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    tool_name    VARCHAR(100) NOT NULL,
    description  TEXT,
    enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    tool_type    VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_agent_tool_company_name UNIQUE (company_id, tool_name)
);

CREATE INDEX idx_agent_tool_company ON agent_tool (company_id);

ALTER TABLE agent_tool ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_tool FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON agent_tool
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- tool_permission — acesso por papel a cada ferramenta. O tenant é herdado
-- da ferramenta (a subquery em agent_tool também passa pelo RLS dela).
-- ===========================================================================
CREATE TABLE tool_permission (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_id      UUID NOT NULL REFERENCES agent_tool(id) ON DELETE CASCADE,
    role         VARCHAR(100) NOT NULL,
    allowed      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tool_permission_tool_role UNIQUE (tool_id, role)
);

ALTER TABLE tool_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE tool_permission FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON tool_permission
    USING (EXISTS (SELECT 1 FROM agent_tool t
                   WHERE t.id = tool_id AND t.company_id = app.current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM agent_tool t
                        WHERE t.id = tool_id AND t.company_id = app.current_tenant_id()));

-- ===========================================================================
-- agent_action_audit — trilha de toda chamada de ferramenta feita pelo agente.
-- ===========================================================================
CREATE TABLE agent_action_audit (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id  UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    tool_name        VARCHAR(100) NOT NULL,
    input            JSONB NOT NULL,
    result           JSONB,
    executed_by      VARCHAR(100) NOT NULL DEFAULT 'ai-agent',
    executed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    outcome          VARCHAR(50) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_action_audit_company ON agent_action_audit (company_id);
CREATE INDEX idx_agent_action_audit_conversation ON agent_action_audit (conversation_id);
CREATE INDEX idx_agent_action_audit_executed_at ON agent_action_audit (executed_at DESC);

ALTER TABLE agent_action_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_action_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON agent_action_audit
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants para crm_app (NOBYPASSRLS; GRANT não afeta RLS). A auditoria é
-- append-only: sem UPDATE/DELETE.
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON agent_tool TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON tool_permission TO crm_app;
GRANT SELECT, INSERT ON agent_action_audit TO crm_app;
