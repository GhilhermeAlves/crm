-- V105__patient_clinical_record.sql
-- Ficha do paciente (Fase 2): prontuário clínico.
--
-- 1. patient_anamneses: anamnese preenchida sobre um snapshot do modelo.
--    Editar o modelo depois NÃO altera anamneses antigas. DRAFT é editável
--    pelo autor/admin; COMPLETED é imutável. Várias por paciente (histórico).
-- 2. patient_evolutions: evolução clínica (rascunho -> finalizada). A finalizada
--    é imutável para todos, inclusive admin; o trigger abaixo é a segunda
--    barreira (o serviço já devolve erro antes).
-- 3. document_templates: modelos de receita/atestado/termo da empresa
--    (Configurações -> Documentos). Corpo com variáveis {{...}}.
-- 4. patient_documents: documento emitido com o texto já renderizado e
--    congelado; imutável e só admin exclui. PDF gerado sob demanda.
-- 5. contacts.photo_storage_id: foto do paciente (objeto de storage_objects).
--
-- company_id desnormalizado + RLS FORCE direta, na mesma linha de V092/V093/
-- V096/V097. Permissões reaproveitadas: contact:read/update (paciente) e
-- anamnesis:read/manage (Configurações -> Documentos).

-- ===========================================================================
-- 1. patient_anamneses — anamnese preenchida (snapshot do modelo + respostas)
-- ===========================================================================
CREATE TABLE patient_anamneses (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id     UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    model_id       UUID REFERENCES anamnesis_models(id) ON DELETE SET NULL,
    model_name     VARCHAR(120) NOT NULL,
    model_version  INT NOT NULL,
    snapshot       JSONB NOT NULL,
    answers        JSONB NOT NULL DEFAULT '{}'::jsonb,
    alerts         JSONB NOT NULL DEFAULT '[]'::jsonb,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    completed_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_anamneses_status CHECK (status IN ('DRAFT', 'COMPLETED')),
    CONSTRAINT chk_patient_anamneses_completed
        CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))
);
CREATE INDEX idx_patient_anamneses_contact
    ON patient_anamneses (company_id, contact_id, created_at DESC);

-- ===========================================================================
-- 2. patient_evolutions — evolução clínica (rascunho -> finalizada imutável)
-- ===========================================================================
CREATE TABLE patient_evolutions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    appointment_id  UUID REFERENCES appointments(id) ON DELETE SET NULL,
    body            TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    finalized_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    finalized_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_evolutions_status CHECK (status IN ('DRAFT', 'FINALIZED')),
    CONSTRAINT chk_patient_evolutions_body CHECK (length(trim(body)) > 0),
    CONSTRAINT chk_patient_evolutions_finalized
        CHECK ((status = 'FINALIZED') = (finalized_at IS NOT NULL))
);
CREATE INDEX idx_patient_evolutions_contact
    ON patient_evolutions (company_id, contact_id, created_at DESC);

-- Evolução finalizada não pode ser alterada nem excluída (prontuário), nem por admin.
-- Exceção: exclusão em cascata do paciente/empresa (pg_trigger_depth() > 1).
CREATE OR REPLACE FUNCTION app.prevent_finalized_evolution_change() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'FINALIZED' AND pg_trigger_depth() = 1 THEN
        IF TG_OP = 'DELETE' THEN
            RAISE EXCEPTION 'Evolução finalizada não pode ser excluída.' USING ERRCODE = 'check_violation';
        END IF;
        RAISE EXCEPTION 'Evolução finalizada não pode ser alterada.' USING ERRCODE = 'check_violation';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_patient_evolutions_immutable
    BEFORE UPDATE OR DELETE ON patient_evolutions
    FOR EACH ROW EXECUTE FUNCTION app.prevent_finalized_evolution_change();

-- ===========================================================================
-- 3. document_templates — modelos de documento da empresa
-- ===========================================================================
CREATE TABLE document_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    title       VARCHAR(160) NOT NULL,
    body        TEXT NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_document_templates_type
        CHECK (type IN ('PRESCRIPTION', 'CERTIFICATE', 'CONSENT', 'OTHER')),
    CONSTRAINT chk_document_templates_body CHECK (length(trim(body)) > 0)
);
CREATE INDEX idx_document_templates_company ON document_templates (company_id, type, is_active);

-- ===========================================================================
-- 4. patient_documents — documento emitido (texto final congelado)
-- ===========================================================================
CREATE TABLE patient_documents (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id             UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id             UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    template_id            UUID REFERENCES document_templates(id) ON DELETE SET NULL,
    type                   VARCHAR(20) NOT NULL,
    title                  VARCHAR(160) NOT NULL,
    body                   TEXT NOT NULL,
    professional_name      VARCHAR(160) NOT NULL,
    professional_registry  VARCHAR(40),
    issued_by              UUID REFERENCES users(id) ON DELETE SET NULL,
    issued_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_documents_type
        CHECK (type IN ('PRESCRIPTION', 'CERTIFICATE', 'CONSENT', 'OTHER')),
    CONSTRAINT chk_patient_documents_body CHECK (length(trim(body)) > 0)
);
CREATE INDEX idx_patient_documents_contact
    ON patient_documents (company_id, contact_id, issued_at DESC);

-- ===========================================================================
-- 5. contacts.photo_storage_id — foto do paciente
-- ===========================================================================
ALTER TABLE contacts
    ADD COLUMN photo_storage_id UUID REFERENCES storage_objects(id) ON DELETE SET NULL;

-- ===========================================================================
-- RLS FORCE + GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['patient_anamneses', 'patient_evolutions',
                             'document_templates', 'patient_documents']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I '
                    || 'USING (company_id = app.current_tenant_id()) '
                    || 'WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO crm_app', t);
    END LOOP;
END $$;
