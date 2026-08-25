-- V056__campaign_tables.sql
-- Sprint 17: Campanhas — aggregate root.
-- Ciclo de vida: DRAFT -> SCHEDULED -> RUNNING (PAUSED) -> COMPLETED | CANCELLED.
-- audience_criteria em JSON (TEXT) + audience_type permitem evolução para
-- segmentação avançada sem mudança de schema (strategy por tipo).

CREATE TABLE campaigns (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                 VARCHAR(120) NOT NULL,
    description          TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN ('DRAFT','SCHEDULED','RUNNING','PAUSED','COMPLETED','CANCELLED')),
    audience_type        VARCHAR(20) NOT NULL DEFAULT 'CONTACTS'   -- CONTACTS | LEADS
                         CHECK (audience_type IN ('CONTACTS','LEADS')),
    audience_criteria    TEXT,                                     -- JSON: {"status":..., "tagIds":[...], "onlyWithPhone":true}
    estimated_recipients INT NOT NULL DEFAULT 0,
    scheduled_at         TIMESTAMP,
    timezone             VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    started_at           TIMESTAMP,
    completed_at         TIMESTAMP,
    created_by           UUID,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_campaigns_company ON campaigns (company_id);
CREATE INDEX idx_campaigns_company_status ON campaigns (company_id, status);
CREATE INDEX idx_campaigns_company_scheduled ON campaigns (company_id, scheduled_at);

ALTER TABLE campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaigns FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON campaigns
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
