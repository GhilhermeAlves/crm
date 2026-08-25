-- V058__campaign_executions.sql
-- Sprint 17: execução de campanha (batch + cursor persistido, sem fila).
-- template_snapshot congela o conteúdo renderizável no início da execução.
-- Idempotência do START é garantida por claim atômico no service
-- (UPDATE ... WHERE status='SCHEDULED') e pela constraint única dos eventos (V059).

CREATE TABLE campaign_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    campaign_id       UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                      CHECK (status IN ('RUNNING','PAUSED','COMPLETED','CANCELLED')),
    template_snapshot TEXT,
    total_recipients  INT NOT NULL DEFAULT 0,
    processed_count   INT NOT NULL DEFAULT 0,
    failed_count      INT NOT NULL DEFAULT 0,
    cursor_offset     INT NOT NULL DEFAULT 0,
    started_at        TIMESTAMP,
    finished_at       TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_campaign_executions_company ON campaign_executions (company_id);
CREATE INDEX idx_campaign_executions_campaign ON campaign_executions (campaign_id);

ALTER TABLE campaign_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_executions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON campaign_executions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
