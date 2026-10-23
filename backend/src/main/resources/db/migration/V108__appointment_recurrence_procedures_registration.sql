-- V108__appointment_recurrence_procedures_registration.sql
-- Novo agendamento completo: recorrência, procedimentos do catálogo e link de
-- cadastro do paciente.
--
-- Rota pública sem sessão (link de cadastro): mesmo mecanismo do convite (V036):
-- GUC transacional app.registration_token (definido por função SECURITY DEFINER)
-- + policies RLS extras que liberam apenas a linha cujo token confere. A empresa
-- é lida da própria linha para então fixar o TenantContext.
-- O valor guardado em `token` é o hash SHA-256 hex (64 chars) do token entregue
-- ao paciente; o token em texto puro nunca é persistido.

-- ===========================================================================
-- 1. Recorrência
-- ===========================================================================
ALTER TABLE appointments ADD COLUMN recurrence_group_id UUID;
CREATE INDEX idx_appointments_recurrence_group ON appointments (recurrence_group_id)
    WHERE recurrence_group_id IS NOT NULL;

ALTER TABLE schedule_blocks ADD COLUMN recurrence_group_id UUID;
CREATE INDEX idx_schedule_blocks_recurrence_group ON schedule_blocks (recurrence_group_id)
    WHERE recurrence_group_id IS NOT NULL;

-- ===========================================================================
-- 2. Procedimentos do agendamento (serviços do catálogo)
-- ===========================================================================
CREATE TABLE appointment_procedures (
    appointment_id  UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL REFERENCES catalog_items(id),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (appointment_id, catalog_item_id)
);

CREATE INDEX idx_appointment_procedures_company ON appointment_procedures (company_id);
CREATE INDEX idx_appointment_procedures_catalog_item ON appointment_procedures (catalog_item_id);

-- ===========================================================================
-- 3. Link de cadastro do paciente
-- ===========================================================================
CREATE TABLE patient_registration_links (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id  UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP,
    created_by  UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_patient_registration_links_token ON patient_registration_links (token);
CREATE INDEX idx_patient_registration_links_company_contact
    ON patient_registration_links (company_id, contact_id);

-- ===========================================================================
-- 4. Contexto de token (padrão V036)
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.set_registration_token_context(p_token text)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    PERFORM set_config('app.registration_token', p_token, true);
END $$;

CREATE OR REPLACE FUNCTION app.current_registration_token()
RETURNS text
LANGUAGE sql STABLE
AS $$
    SELECT nullif(current_setting('app.registration_token', true), '');
$$;

GRANT EXECUTE ON FUNCTION app.set_registration_token_context(text) TO crm_app;
GRANT EXECUTE ON FUNCTION app.current_registration_token() TO crm_app;

-- ===========================================================================
-- 5. RLS FORCE + GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['appointment_procedures', 'patient_registration_links']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I '
                    || 'USING (company_id = app.current_tenant_id()) '
                    || 'WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO crm_app', t);
    END LOOP;
END $$;

-- Acesso público por token: quem apresenta o token lê/atualiza (marca used_at)
-- somente aquele link, sem sessão de tenant.
CREATE POLICY registration_token_select_policy ON patient_registration_links
    FOR SELECT USING (token = app.current_registration_token());

CREATE POLICY registration_token_update_policy ON patient_registration_links
    FOR UPDATE USING (token = app.current_registration_token())
    WITH CHECK (token = app.current_registration_token());
