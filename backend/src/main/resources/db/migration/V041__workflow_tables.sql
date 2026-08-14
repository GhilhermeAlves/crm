-- V041__workflow_tables.sql
-- Sprint 14: Workflow e Automação Comercial — núcleo do mecanismo.
--
-- Segue o padrão estabelecido (V016/V017/V039): company_id NOT NULL FK para
-- companies, timestamps NOT NULL, índice por company_id e RLS FORCE + policy
-- tenant_isolation_policy usando app.current_tenant_id().
--
-- Os grants CRUD para o role crm_app são aplicados automaticamente pelo loop
-- dinâmico da V034 (pg_tables public), portanto NÃO precisamos de GRANT aqui.

-- ===========================================================================
-- workflows — regra de automação pertencente a uma Company (multi-tenant).
-- ===========================================================================
CREATE TABLE workflows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    trigger         VARCHAR(40) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workflows_company_id       ON workflows (company_id);
CREATE INDEX idx_workflows_company_active   ON workflows (company_id, active);
CREATE INDEX idx_workflows_company_trigger  ON workflows (company_id, trigger);

ALTER TABLE workflows ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflows FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON workflows
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- workflow_conditions — condições da regra (field + operator + value).
-- ===========================================================================
CREATE TABLE workflow_conditions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    field       VARCHAR(60) NOT NULL,
    operator    VARCHAR(25) NOT NULL,
    value       TEXT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_workflow_conditions_workflow_id ON workflow_conditions (workflow_id);
CREATE INDEX idx_workflow_conditions_company_id  ON workflow_conditions (company_id);

ALTER TABLE workflow_conditions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_conditions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON workflow_conditions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- workflow_actions — ações da regra (CreateTask / CreateActivity). O campo
-- config guarda a configuração da ação em JSON (ex.: título, prazo em dias,
-- prioridade), permitindo evolução sem reconstrução (Item 15).
-- ===========================================================================
CREATE TABLE workflow_actions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    action_type VARCHAR(30) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    config      TEXT NOT NULL
);

CREATE INDEX idx_workflow_actions_workflow_id ON workflow_actions (workflow_id);
CREATE INDEX idx_workflow_actions_company_id  ON workflow_actions (company_id);

ALTER TABLE workflow_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_actions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON workflow_actions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- workflow_executions — histórico/idempotência. A chave única
-- (company_id, workflow_action_id, event_id) garante que o MESMO
-- evento + MESMA ação nunca execute duas vezes (Item 6). workflow_action_id
-- NÃO possui FK para workflow_actions de propósito: assim o histórico de
-- execução sobrevive à edição/exclusão de uma regra (rastreabilidade).
-- ===========================================================================
CREATE TABLE workflow_executions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id         UUID REFERENCES workflows(id) ON DELETE SET NULL,
    workflow_action_id  UUID NOT NULL,
    event_id            UUID NOT NULL,
    event_type          VARCHAR(40) NOT NULL,
    entity_id           UUID,
    action_type         VARCHAR(30) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    result_text         TEXT,
    error_message       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE workflow_executions ADD CONSTRAINT uq_workflow_executions_dedup
    UNIQUE (company_id, workflow_action_id, event_id);

CREATE INDEX idx_workflow_executions_company_id     ON workflow_executions (company_id);
CREATE INDEX idx_workflow_executions_workflow_id    ON workflow_executions (workflow_id);
CREATE INDEX idx_workflow_executions_event_type     ON workflow_executions (event_type);
CREATE INDEX idx_workflow_executions_created_at     ON workflow_executions (created_at);

ALTER TABLE workflow_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_executions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON workflow_executions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());