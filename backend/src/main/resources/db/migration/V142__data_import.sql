-- Importação de base de outro sistema (spec 2026-11-19). RLS por empresa.

CREATE TABLE data_import (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID,
    kind        VARCHAR(30)  NOT NULL,
    source_file VARCHAR(255),
    total       INT NOT NULL DEFAULT 0,
    imported    INT NOT NULL DEFAULT 0,
    skipped     INT NOT NULL DEFAULT 0,
    failed      INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    undone_at   TIMESTAMP
);
CREATE INDEX idx_data_import_company ON data_import (company_id, created_at DESC);

CREATE TABLE data_import_contacts (
    import_id  UUID NOT NULL REFERENCES data_import(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (import_id, contact_id)
);

ALTER TABLE data_import ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_import FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON data_import
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON data_import TO crm_app;

ALTER TABLE data_import_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_import_contacts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON data_import_contacts
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON data_import_contacts TO crm_app;
