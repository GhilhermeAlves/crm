-- V110__trichoscopy_and_invitation_specialty.sql
-- Tricoscopia (captura via tricoscópio USB/UVC) + especialidade em convites/memberships.

-- 1) Novo segmento de empresa: tricologia / medicina capilar
ALTER TABLE companies DROP CONSTRAINT chk_companies_segment;
ALTER TABLE companies
    ADD CONSTRAINT chk_companies_segment
        CHECK (segment IN ('GENERIC', 'DENTAL', 'OCCUPATIONAL_SAFETY', 'TRICHOLOGY'));

-- 2) Especialidade do profissional em convites e memberships (opcional)
ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS specialty VARCHAR(30)
        CONSTRAINT chk_invitations_specialty CHECK (specialty IN ('GENERIC', 'DENTAL', 'TRICHOLOGY'));

ALTER TABLE memberships
    ADD COLUMN IF NOT EXISTS specialty VARCHAR(30)
        CONSTRAINT chk_memberships_specialty CHECK (specialty IN ('GENERIC', 'DENTAL', 'TRICHOLOGY'));

-- 3) Imagens de tricoscopia por paciente
CREATE TABLE patient_trichoscopy_images (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id        UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    storage_object_id UUID NOT NULL REFERENCES storage_objects(id) ON DELETE RESTRICT,
    taken_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    taken_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes             TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patient_trichoscopy_images_company_contact
    ON patient_trichoscopy_images (company_id, contact_id, taken_at DESC);
CREATE INDEX idx_patient_trichoscopy_images_storage
    ON patient_trichoscopy_images (storage_object_id);

-- RLS FORCE + GRANT (padrão V105)
ALTER TABLE patient_trichoscopy_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_trichoscopy_images FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON patient_trichoscopy_images
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON patient_trichoscopy_images TO crm_app;
