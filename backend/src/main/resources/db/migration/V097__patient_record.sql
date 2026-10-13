-- V097__patient_record.sql
-- Ficha do paciente (Fase 1): cadastro completo do contato, anotações e vínculo
-- de arquivos ao paciente.
--
-- 1. Campos de cadastro clínico/administrativo em contacts: endereço, plano de
--    saúde, nº do prontuário, referência (como conheceu) e responsável.
--    record_number é texto livre opcional, único por empresa quando preenchido
--    (índice único parcial).
-- 2. contact_notes: lista de anotações do paciente com autor e data (distinta do
--    campo legado contacts.notes, que continua existindo como observação única).
-- 3. storage_objects.contact_id: permite vincular um arquivo a um paciente; os
--    arquivos da empresa (sem contato) continuam com contact_id NULL.
--
-- company_id desnormalizado + RLS FORCE direta, na mesma linha das demais
-- features (V092/V093/V096). Permissões reaproveitadas: contact:read/update.

-- ===========================================================================
-- 1. contacts — cadastro do paciente
-- ===========================================================================
ALTER TABLE contacts
    ADD COLUMN zip_code           VARCHAR(10),
    ADD COLUMN street             VARCHAR(160),
    ADD COLUMN address_number     VARCHAR(20),
    ADD COLUMN address_complement VARCHAR(80),
    ADD COLUMN neighborhood       VARCHAR(80),
    ADD COLUMN city               VARCHAR(80),
    ADD COLUMN state              CHAR(2),
    ADD COLUMN health_plan        VARCHAR(80),
    ADD COLUMN health_plan_number VARCHAR(40),
    ADD COLUMN record_number      VARCHAR(40),
    ADD COLUMN referral_source    VARCHAR(80),
    ADD COLUMN has_guardian       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guardian_name      VARCHAR(160),
    ADD COLUMN guardian_cpf       VARCHAR(14),
    ADD COLUMN guardian_phone     VARCHAR(20);

CREATE UNIQUE INDEX uq_contacts_company_record_number
    ON contacts (company_id, record_number)
    WHERE record_number IS NOT NULL;

-- ===========================================================================
-- 2. contact_notes — anotações do paciente
-- ===========================================================================
CREATE TABLE contact_notes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id  UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    body        TEXT NOT NULL,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contact_notes_body CHECK (length(trim(body)) > 0)
);

CREATE INDEX idx_contact_notes_contact ON contact_notes (company_id, contact_id, created_at DESC);

ALTER TABLE contact_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact_notes FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON contact_notes
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON contact_notes TO crm_app;

-- ===========================================================================
-- 3. storage_objects — vínculo com o paciente
-- ===========================================================================
ALTER TABLE storage_objects
    ADD COLUMN contact_id UUID REFERENCES contacts(id) ON DELETE CASCADE;

CREATE INDEX idx_storage_objects_company_contact ON storage_objects (company_id, contact_id);
