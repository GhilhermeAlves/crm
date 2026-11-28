-- V153__trichology_ai_runs_and_verification.sql
-- 1) Registro de cada chamada de IA do módulo (sem a imagem: só o hash).
-- 2) Profissional que assina o relatório (como em patient_documents).
-- 3) Acesso público: quiz por token (policy pelo GUC app.form_token, padrão V115)
--    e verificação do relatório por código (função SECURITY DEFINER que devolve
--    só dados não clínicos).

CREATE TABLE trichology_ai_runs (
    id                  UUID PRIMARY KEY,
    company_id          UUID NOT NULL REFERENCES companies (id),
    assessment_id       UUID NOT NULL,
    assessment_image_id UUID,
    stage               VARCHAR(20) NOT NULL,
    model               VARCHAR(60),
    prompt_version      VARCHAR(20) NOT NULL,
    image_sha256        VARCHAR(64),
    input_tokens        INT,
    output_tokens       INT,
    status              VARCHAR(10) NOT NULL,
    error_code          VARCHAR(60),
    latency_ms          INT,
    created_by          UUID REFERENCES users (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_trichology_ai_runs_assessment FOREIGN KEY (company_id, assessment_id)
        REFERENCES trichology_assessments (company_id, id),
    CONSTRAINT fk_trichology_ai_runs_image FOREIGN KEY (company_id, assessment_image_id)
        REFERENCES trichology_assessment_images (company_id, id),
    CONSTRAINT ck_trichology_ai_runs_stage CHECK (
        stage IN ('IMAGE_ANALYSIS', 'COPILOT', 'EXAM_SUGGESTION', 'REPORT_SECTION')),
    CONSTRAINT ck_trichology_ai_runs_status CHECK (status IN ('OK', 'FAILED', 'BLOCKED'))
);
CREATE INDEX idx_trichology_ai_runs_assessment ON trichology_ai_runs (company_id, assessment_id, created_at);

ALTER TABLE trichology_ai_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE trichology_ai_runs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON trichology_ai_runs
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT ON trichology_ai_runs TO crm_app;

ALTER TABLE trichology_assessments
    ADD COLUMN professional_name     VARCHAR(160),
    ADD COLUMN professional_registry VARCHAR(40);

-- Quiz pelo link: quem apresenta o token lê somente aquele exame (a gravação
-- acontece depois, já com o TenantContext da empresa lida na linha).
CREATE POLICY quiz_token_select_policy ON trichology_assessments
    FOR SELECT USING (quiz_form_token_hash = app.current_form_token());

-- Verificação pública: sem dados clínicos nem do paciente.
CREATE OR REPLACE FUNCTION app.trichology_verify(p_code text)
RETURNS TABLE (
    published_at          TIMESTAMPTZ,
    professional_name     VARCHAR,
    professional_registry VARCHAR,
    company_name          VARCHAR,
    report_version        INT,
    pdf_sha256            VARCHAR,
    superseded            BOOLEAN
)
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT a.published_at, a.professional_name, a.professional_registry, c.trading_name::VARCHAR,
           a.report_version, a.pdf_sha256,
           EXISTS (SELECT 1 FROM trichology_assessments n
                    WHERE n.supersedes_id = a.id AND n.status = 'PUBLISHED')
      FROM trichology_assessments a
      JOIN companies c ON c.id = a.company_id
     WHERE a.verification_code = upper(p_code)
       AND a.status = 'PUBLISHED';
$$;
REVOKE ALL ON FUNCTION app.trichology_verify(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.trichology_verify(text) TO crm_app;
