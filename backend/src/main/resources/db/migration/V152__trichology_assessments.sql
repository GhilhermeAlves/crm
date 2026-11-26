-- V152__trichology_assessments.sql
-- Exame tricológico (doc v1.2, seções 2 e 5). contacts e persons compartilham o
-- mesmo UUID (V147, item 8), então contact_id também referencia persons.

CREATE TABLE trichology_assessments (
    id                    UUID PRIMARY KEY,
    company_id            UUID NOT NULL REFERENCES companies (id),
    contact_id            UUID NOT NULL,
    professional_id       UUID NOT NULL REFERENCES users (id),
    protocol_id           UUID,
    status                VARCHAR(30) NOT NULL,
    consent_at            TIMESTAMPTZ,
    consent_by            UUID REFERENCES users (id),
    chief_complaint       TEXT,
    quiz_answers          JSONB,
    quiz_completed_at     TIMESTAMPTZ,
    quiz_form_token_hash  VARCHAR(64),
    quiz_form_expires_at  TIMESTAMPTZ,
    clinical_notes        TEXT,
    pull_test             JSONB,
    trichoscopy_performed BOOLEAN NOT NULL DEFAULT FALSE,
    skipped_regions       JSONB NOT NULL DEFAULT '[]'::jsonb,
    requested_exams       JSONB,
    lab_results           JSONB,
    lab_collected_on      DATE,
    rps_summary           JSONB,
    ai_draft_payload      JSONB,
    final_report_payload  JSONB,
    report_version        INT NOT NULL DEFAULT 1,
    supersedes_id         UUID,
    patient_document_id   UUID REFERENCES patient_documents (id),
    verification_code     VARCHAR(12),
    pdf_sha256            VARCHAR(64),
    pdf_storage_object_id UUID REFERENCES storage_objects (id),
    reviewed_by           UUID REFERENCES users (id),
    reviewed_at           TIMESTAMPTZ,
    published_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    cancel_reason         TEXT,
    created_by            UUID NOT NULL REFERENCES users (id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_trichology_assessments_company_id UNIQUE (company_id, id),
    CONSTRAINT fk_trichology_assessments_contact FOREIGN KEY (company_id, contact_id)
        REFERENCES contacts (company_id, id),
    CONSTRAINT fk_trichology_assessments_person FOREIGN KEY (company_id, contact_id)
        REFERENCES persons (company_id, id),
    CONSTRAINT fk_trichology_assessments_protocol FOREIGN KEY (company_id, protocol_id)
        REFERENCES trichology_capture_protocols (company_id, id),
    CONSTRAINT fk_trichology_assessments_supersedes FOREIGN KEY (company_id, supersedes_id)
        REFERENCES trichology_assessments (company_id, id),
    CONSTRAINT ck_trichology_assessments_status CHECK (status IN (
        'DRAFT', 'QUIZ_COMPLETED', 'IMAGING_IN_PROGRESS', 'IMAGING_DONE', 'CLINICAL_DONE',
        'EXAMS_REQUESTED', 'LABS_LOADED', 'AI_DRAFT_READY', 'GUARDRAIL_BLOCKED', 'REVIEWED',
        'PUBLISHED', 'CANCELLED')),
    CONSTRAINT ck_trichology_assessments_version CHECK (report_version >= 1),
    CONSTRAINT ck_trichology_assessments_published CHECK (
        status <> 'PUBLISHED' OR (published_at IS NOT NULL AND verification_code IS NOT NULL
                                  AND pdf_sha256 IS NOT NULL AND final_report_payload IS NOT NULL))
);
CREATE INDEX idx_trichology_assessments_contact
    ON trichology_assessments (company_id, contact_id, created_at DESC);
-- Código de verificação é global: a rota pública não conhece a empresa.
CREATE UNIQUE INDEX uq_trichology_assessments_verification
    ON trichology_assessments (verification_code) WHERE verification_code IS NOT NULL;
CREATE UNIQUE INDEX uq_trichology_assessments_quiz_token
    ON trichology_assessments (quiz_form_token_hash) WHERE quiz_form_token_hash IS NOT NULL;

CREATE TABLE trichology_assessment_images (
    id                 UUID PRIMARY KEY,
    company_id         UUID NOT NULL REFERENCES companies (id),
    assessment_id      UUID NOT NULL,
    image_id           UUID NOT NULL,
    region_code        VARCHAR(40) NOT NULL,
    magnification      VARCHAR(10),
    quality_status     VARCHAR(10) NOT NULL DEFAULT 'UNCHECKED',
    ai_status          VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    ai_image_quality   VARCHAR(10),
    ai_findings        JSONB,
    ai_estimates       JSONB,
    ai_notes           TEXT,
    accepted_findings  JSONB NOT NULL DEFAULT '[]'::jsonb,
    confirmed_measures JSONB NOT NULL DEFAULT '{}'::jsonb,
    include_in_report  BOOLEAN NOT NULL DEFAULT TRUE,
    reviewed_by        UUID REFERENCES users (id),
    reviewed_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_trichology_assessment_images_company_id UNIQUE (company_id, id),
    CONSTRAINT uq_trichology_assessment_images_image UNIQUE (assessment_id, image_id),
    CONSTRAINT fk_trichology_assessment_images_assessment FOREIGN KEY (company_id, assessment_id)
        REFERENCES trichology_assessments (company_id, id),
    CONSTRAINT fk_trichology_assessment_images_image FOREIGN KEY (company_id, image_id)
        REFERENCES patient_trichoscopy_images (company_id, id),
    CONSTRAINT ck_trichology_assessment_images_quality CHECK (
        quality_status IN ('UNCHECKED', 'OK', 'BLURRED', 'DARK', 'GLARE')),
    CONSTRAINT ck_trichology_assessment_images_ai_quality CHECK (
        ai_image_quality IS NULL OR ai_image_quality IN ('OK', 'BLURRED', 'DARK', 'GLARE')),
    CONSTRAINT ck_trichology_assessment_images_ai_status CHECK (
        ai_status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED', 'SKIPPED'))
);
CREATE INDEX idx_trichology_assessment_images_assessment
    ON trichology_assessment_images (company_id, assessment_id);
CREATE INDEX idx_trichology_assessment_images_image
    ON trichology_assessment_images (company_id, image_id);

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['trichology_assessments', 'trichology_assessment_images'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I
                            USING (company_id = app.current_tenant_id())
                            WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE ON %I TO crm_app', t);
    END LOOP;
END $$;
