-- V115__patient_form_requests.sql
-- Formulário (anamnese) respondido pelo próprio paciente, por link ou no modo
-- recepção (tablet). Cada pedido aponta para uma anamnese em rascunho criada no
-- envio; o paciente responde sem login e assina no fim.
--
-- Rota pública sem sessão: mesmo mecanismo do link de cadastro (V108) — GUC
-- transacional app.form_token (definido por função SECURITY DEFINER) + policies
-- que liberam só a linha cujo token confere. A empresa é lida da própria linha
-- para então fixar o TenantContext. Em `token` fica o hash SHA-256 hex (64
-- chars); o token em texto puro nunca é persistido.

CREATE TABLE patient_form_requests (
    id                    UUID PRIMARY KEY,
    company_id            UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id            UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    -- Descartar o rascunho da anamnese cancela o pedido (a linha some junto).
    anamnesis_id          UUID NOT NULL REFERENCES patient_anamneses(id) ON DELETE CASCADE,
    token                 VARCHAR(64) NOT NULL,
    mode                  VARCHAR(20) NOT NULL
        CONSTRAINT chk_patient_form_requests_mode CHECK (mode IN ('LINK', 'RECEPTION')),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_patient_form_requests_status CHECK (status IN ('PENDING', 'ANSWERED')),
    expires_at            TIMESTAMP NOT NULL,
    answered_at           TIMESTAMP,
    signature_storage_id  UUID REFERENCES storage_objects(id) ON DELETE SET NULL,
    created_by            UUID,
    created_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_patient_form_requests_token ON patient_form_requests (token);
CREATE INDEX idx_patient_form_requests_company_contact
    ON patient_form_requests (company_id, contact_id, created_at DESC);
CREATE INDEX idx_patient_form_requests_anamnesis ON patient_form_requests (anamnesis_id);

-- Contexto de token (padrão V036/V108)
CREATE OR REPLACE FUNCTION app.set_form_token_context(p_token text)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    PERFORM set_config('app.form_token', p_token, true);
END $$;

CREATE OR REPLACE FUNCTION app.current_form_token()
RETURNS text
LANGUAGE sql STABLE
AS $$
    SELECT nullif(current_setting('app.form_token', true), '');
$$;

GRANT EXECUTE ON FUNCTION app.set_form_token_context(text) TO crm_app;
GRANT EXECUTE ON FUNCTION app.current_form_token() TO crm_app;

ALTER TABLE patient_form_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_form_requests FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON patient_form_requests
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON patient_form_requests TO crm_app;

-- Acesso público por token: quem apresenta o token lê somente aquele pedido.
CREATE POLICY form_token_select_policy ON patient_form_requests
    FOR SELECT USING (token = app.current_form_token());
