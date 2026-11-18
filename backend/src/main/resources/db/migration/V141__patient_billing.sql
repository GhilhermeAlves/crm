-- Cobrança de pacientes pelo Asaas (spec 2026-11-18). Tabelas próprias, RLS por empresa.

CREATE TABLE patient_billing_companies (
    company_id UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    enabled    BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE patient_billing_customers (
    contact_id        UUID PRIMARY KEY,
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    asaas_customer_id VARCHAR(100) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE patient_charges (
    transaction_id   UUID PRIMARY KEY,
    company_id       UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    asaas_payment_id VARCHAR(100) UNIQUE,
    invoice_url      VARCHAR(500),
    status           VARCHAR(30) NOT NULL,
    error            VARCHAR(500),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_patient_charges_company ON patient_charges (company_id);

ALTER TABLE patient_billing_companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_billing_companies FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON patient_billing_companies
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON patient_billing_companies TO crm_app;

ALTER TABLE patient_billing_customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_billing_customers FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON patient_billing_customers
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON patient_billing_customers TO crm_app;

ALTER TABLE patient_charges ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_charges FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON patient_charges
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON patient_charges TO crm_app;
