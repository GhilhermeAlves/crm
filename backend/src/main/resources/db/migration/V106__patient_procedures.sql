-- ===========================================================================
-- V106 — Ficha clínica do paciente (ficha do paciente, Fase 3a)
--
-- patient_procedures: procedimento odontológico do paciente (serviço do
--   catálogo), em um de três estados: PLANNED (a realizar), DONE (realizado),
--   PRE_EXISTING (pré-existente). Pode gerar um lançamento em
--   financial_transactions quando DONE.
-- patient_procedure_teeth: dentes (FDI) e faces de cada procedimento. Sem
--   linhas = procedimento da boca toda.
-- Permissões reaproveitadas: contact:read / contact:update (+ finance:create
-- para gerar lançamento). O vínculo com orçamento (budget_id) chega na 3b.
-- ===========================================================================
CREATE TABLE patient_procedures (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id               UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    catalog_item_id          UUID NOT NULL REFERENCES catalog_items(id),
    name_snapshot            VARCHAR(160) NOT NULL,
    state                    VARCHAR(20) NOT NULL,
    dentition                VARCHAR(10) NOT NULL,
    price_cents              BIGINT NOT NULL DEFAULT 0,
    professional_id          UUID REFERENCES users(id) ON DELETE SET NULL,
    notes                    TEXT,
    performed_at             DATE,
    financial_transaction_id UUID REFERENCES financial_transactions(id) ON DELETE SET NULL,
    created_by               UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_procedures_state CHECK (state IN ('PLANNED', 'DONE', 'PRE_EXISTING')),
    CONSTRAINT chk_patient_procedures_dentition CHECK (dentition IN ('PERMANENT', 'DECIDUOUS')),
    CONSTRAINT chk_patient_procedures_price CHECK (price_cents >= 0),
    CONSTRAINT chk_patient_procedures_performed CHECK (state <> 'DONE' OR performed_at IS NOT NULL)
);

CREATE INDEX idx_patient_procedures_company_contact ON patient_procedures (company_id, contact_id);

CREATE TABLE patient_procedure_teeth (
    procedure_id UUID NOT NULL REFERENCES patient_procedures(id) ON DELETE CASCADE,
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    tooth        SMALLINT NOT NULL,
    faces        VARCHAR(1)[] NOT NULL DEFAULT '{}',
    PRIMARY KEY (procedure_id, tooth),
    CONSTRAINT chk_patient_procedure_teeth_faces CHECK (faces <@ ARRAY['M', 'D', 'O', 'V', 'L']::VARCHAR(1)[])
);

-- ===========================================================================
-- RLS FORCE + GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['patient_procedures', 'patient_procedure_teeth']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I '
                    || 'USING (company_id = app.current_tenant_id()) '
                    || 'WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO crm_app', t);
    END LOOP;
END $$;
