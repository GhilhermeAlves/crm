-- V016__leads_tables.sql

CREATE TABLE leads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    score           INTEGER NOT NULL DEFAULT 0,
    classification  VARCHAR(20),
    source          VARCHAR(20) NOT NULL,
    campaign_id     UUID,
    assigned_to     UUID REFERENCES users(id) ON DELETE SET NULL,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_leads_contact_company ON leads (contact_id, company_id);
CREATE INDEX idx_leads_company_id ON leads (company_id);
CREATE INDEX idx_leads_status ON leads (status);
CREATE INDEX idx_leads_score ON leads (score);
CREATE INDEX idx_leads_assigned_to ON leads (assigned_to);
CREATE INDEX idx_leads_source ON leads (source);

ALTER TABLE leads ADD CONSTRAINT chk_leads_score CHECK (score >= 0 AND score <= 100);
ALTER TABLE leads ADD CONSTRAINT chk_leads_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'UNQUALIFIED', 'CONVERTED', 'LOST'));
ALTER TABLE leads ADD CONSTRAINT chk_leads_source CHECK (source IN ('WHATSAPP', 'FORM', 'API', 'IMPORT', 'MANUAL'));
