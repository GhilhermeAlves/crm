-- V047__notifications.sql
-- Módulo de Notificações: tabela de notificações in-app pessoais.
--
-- Segue o padrão de V039 (tasks): company_id NOT NULL FK para companies,
-- timestamps, RLS FORCE + policy tenant_isolation_policy usando
-- app.current_tenant_id().
--
-- As notificações são PESSOAIS: além do isolamento por empresa, cada linha tem
-- user_id (destinatário). A autorização por usuário é feita no serviço
-- (defense-in-depth) — o RLS garante isolamento por empresa.
--
-- Os grants CRUD para o role crm_app são aplicados automaticamente pelo loop
-- dinâmico da V034, portanto NÃO precisamos de GRANT aqui.

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL DEFAULT 'INFO',
    title           VARCHAR(200) NOT NULL,
    body            TEXT,
    metadata        TEXT,
    read_at         TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type CHECK (type IN (
    'TASK', 'WORKFLOW', 'INVITATION', 'MESSAGE', 'LEAD', 'OPPORTUNITY', 'SYSTEM', 'INFO'
));

CREATE INDEX idx_notifications_company_id  ON notifications (company_id);
CREATE INDEX idx_notifications_user_id     ON notifications (user_id);
CREATE INDEX idx_notifications_company_user_read
    ON notifications (company_id, user_id, read_at);
CREATE INDEX idx_notifications_created_at  ON notifications (created_at);

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON notifications
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());