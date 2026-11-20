-- Importação de procedimentos de outro sistema (fase 2). Guarda o código de
-- origem do paciente e rastreia procedimentos/serviços criados por importação.

ALTER TABLE data_import_contacts ADD COLUMN source_id VARCHAR(64);
CREATE INDEX idx_data_import_contacts_source ON data_import_contacts (company_id, source_id);

CREATE TABLE data_import_procedures (
    import_id    UUID NOT NULL REFERENCES data_import(id) ON DELETE CASCADE,
    procedure_id UUID NOT NULL REFERENCES patient_procedures(id) ON DELETE CASCADE,
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    source_id    VARCHAR(64),
    PRIMARY KEY (import_id, procedure_id)
);
CREATE INDEX idx_data_import_procedures_source ON data_import_procedures (company_id, source_id);

CREATE TABLE data_import_catalog_items (
    import_id       UUID NOT NULL REFERENCES data_import(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (import_id, catalog_item_id)
);

ALTER TABLE data_import_procedures ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_import_procedures FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON data_import_procedures
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON data_import_procedures TO crm_app;

ALTER TABLE data_import_catalog_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_import_catalog_items FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON data_import_catalog_items
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON data_import_catalog_items TO crm_app;
