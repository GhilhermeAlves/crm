-- V039__activities_tasks_tables.sql
-- Sprint 12: tabelas remanescentes de CRM orientado à ação (Activities + Tasks).
--
-- Segue o padrão de V016 (leads) / V017 (pipeline): company_id NOT NULL FK para
-- companies, timestamps NOT NULL, CHECK constraints separadas e, principalmente,
-- RLS FORCE + policy tenant_isolation_policy usando app.current_tenant_id().
--
-- Os grants CRUD para o role crm_app são aplicados automaticamente pelo loop
-- dinâmico da V034 (pg_tables public), portanto NÃO precisamos de GRANT aqui.

-- ===========================================================================
-- Activities (Timeline)
-- ===========================================================================
-- Uma Activity representa uma interação/acontecimento comercial. Pode estar
-- vinculada opcionalmente a um Contact e/ou a uma Opportunity (ambos de MUITOS
-- para poucos, nullable) — isso permite tromar Activities num timeline unificada
-- e, no futuro, ingerir eventos do Inbox (email/whatsapp) sem reorganizar o
-- modelo (o Inbox apenas cria Activities do tipo correspondente).
CREATE TABLE activities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES contacts(id)   ON DELETE SET NULL,
    opportunity_id  UUID REFERENCES opportunities(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    description     TEXT,
    activity_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE activities ADD CONSTRAINT chk_activities_type CHECK (type IN (
    'CALL', 'MEETING', 'EMAIL', 'MESSAGE', 'NOTE', 'PROPOSAL', 'FOLLOW_UP', 'OTHER'
));

CREATE INDEX idx_activities_company_id       ON activities (company_id);
CREATE INDEX idx_activities_contact_id       ON activities (contact_id);
CREATE INDEX idx_activities_opportunity_id   ON activities (opportunity_id);
CREATE INDEX idx_activities_type             ON activities (type);
CREATE INDEX idx_activities_activity_at      ON activities (activity_at);

ALTER TABLE activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE activities FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON activities
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Tasks (Follow-up)
-- ===========================================================================
-- Uma Task representa uma ação a executar. Relacionamentos (contact/opportunity)
-- são nullable e não-enumerados — permite associar a outras entidades (Company,
-- Activity) no futuro sem alterar o modelo (não acoplar a um único tipo).
CREATE TABLE tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES contacts(id)   ON DELETE SET NULL,
    opportunity_id  UUID REFERENCES opportunities(id) ON DELETE SET NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    assignee_id     UUID,
    due_at          TIMESTAMP,
    priority        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tasks ADD CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

CREATE INDEX idx_tasks_company_id      ON tasks (company_id);
CREATE INDEX idx_tasks_contact_id      ON tasks (contact_id);
CREATE INDEX idx_tasks_opportunity_id  ON tasks (opportunity_id);
CREATE INDEX idx_tasks_assignee_id     ON tasks (assignee_id);
CREATE INDEX idx_tasks_status          ON tasks (status);
CREATE INDEX idx_tasks_due_at          ON tasks (due_at);

ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON tasks
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());