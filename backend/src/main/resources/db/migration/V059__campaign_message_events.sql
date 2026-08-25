-- V059__campaign_message_events.sql
-- Sprint 17: evento por destinatário — base de métricas (Sprint 19) e
-- GARANTIA DE IDEMPOTÊNCIA: UNIQUE (execution_id, recipient_id) impede
-- envio duplicado ao mesmo destinatário mesmo sob retry/concorrência/restart.

CREATE TABLE campaign_message_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    execution_id        UUID NOT NULL REFERENCES campaign_executions(id) ON DELETE CASCADE,
    campaign_id         UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    recipient_id        UUID NOT NULL,
    recipient_type      VARCHAR(10) NOT NULL DEFAULT 'CONTACT'    -- CONTACT | LEAD
                        CHECK (recipient_type IN ('CONTACT','LEAD')),
    recipient_phone     VARCHAR(40),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','SENT','FAILED','CANCELLED','DELIVERED','READ','RESPONDED','OPTED_OUT')),
    attempts            INT NOT NULL DEFAULT 0,
    error_reason        TEXT,
    provider_message_id VARCHAR(120),
    occurred_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_campaign_message_events_execution_recipient
    ON campaign_message_events (execution_id, recipient_id);
CREATE INDEX idx_campaign_message_events_company ON campaign_message_events (company_id);
CREATE INDEX idx_campaign_message_events_company_campaign ON campaign_message_events (company_id, campaign_id, status);

ALTER TABLE campaign_message_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_message_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON campaign_message_events
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
