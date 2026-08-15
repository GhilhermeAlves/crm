-- V043__workflow_runs.sql
-- Sprint 15: Observabilidade do Workflow — registro de execução em nível de RULE.
--
-- Complementa workflow_executions (por AÇÃO) com um registro por (empresa, workflow,
-- evento) que captura: condições avaliadas (esperado x encontrado), contexto seguro
-- do evento e o motivo do skip. Assim o usuário responde "por que esta regra
-- executou / não executou?" — inclusive quando a regra é ignorada por condição
-- (caso em que workflow_executions não possui linha alguma).
--
-- Idempotência própria: chave única (company_id, workflow_id, event_id). Segue o
-- padrão V041: company_id NOT NULL FK companies, timestamps, índices, RLS FORCE +
-- tenant_isolation_policy via app.current_tenant_id(). Grants CRUD do role crm_app
-- são aplicados pelo loop dinâmico da V034 (não precisamos de GRANT aqui).

CREATE TABLE workflow_runs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    event_id    UUID NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    entity_id   UUID,
    status      VARCHAR(20) NOT NULL,
    conditions  TEXT,
    context     TEXT,
    result_text TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE workflow_runs ADD CONSTRAINT uq_workflow_runs_dedup
    UNIQUE (company_id, workflow_id, event_id);

CREATE INDEX idx_workflow_runs_company_created ON workflow_runs (company_id, created_at);
CREATE INDEX idx_workflow_runs_workflow_id     ON workflow_runs (workflow_id);
CREATE INDEX idx_workflow_runs_status          ON workflow_runs (status);
CREATE INDEX idx_workflow_runs_event_type      ON workflow_runs (event_type);

ALTER TABLE workflow_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_runs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON workflow_runs
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
