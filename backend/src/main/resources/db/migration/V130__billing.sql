-- V130__billing.sql
-- Cobrança automática (Asaas): conta por empresa, faturas e idempotência do webhook.

CREATE TABLE billing_accounts (
    company_id            UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    status                VARCHAR(20) NOT NULL,
    plan                  VARCHAR(20) NOT NULL,
    monthly_price_cents   INTEGER NOT NULL CHECK (monthly_price_cents > 0),
    asaas_customer_id     VARCHAR(64),
    asaas_subscription_id VARCHAR(64),
    next_due_date         DATE,
    overdue_since         DATE,
    grace_until           DATE,
    sync_error            VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE billing_invoices (
    id               UUID PRIMARY KEY,
    company_id       UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    asaas_payment_id VARCHAR(64) NOT NULL UNIQUE,
    due_date         DATE NOT NULL,
    value_cents      INTEGER NOT NULL,
    status           VARCHAR(20) NOT NULL,
    invoice_url      VARCHAR(500),
    paid_at          TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_billing_invoices_company_due ON billing_invoices(company_id, due_date DESC);

-- Tabela de plataforma (sem company_id/RLS): só o webhook escreve.
CREATE TABLE billing_webhook_events (
    asaas_event_id VARCHAR(100) PRIMARY KEY,
    event          VARCHAR(60) NOT NULL,
    received_at    TIMESTAMP NOT NULL DEFAULT now(),
    processed_at   TIMESTAMP
);
GRANT SELECT, INSERT, UPDATE ON billing_webhook_events TO crm_app;

ALTER TABLE billing_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing_accounts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON billing_accounts
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON billing_accounts TO crm_app;

ALTER TABLE billing_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing_invoices FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON billing_invoices
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON billing_invoices TO crm_app;
