-- V133__companies_rls.sql
-- RLS em companies. Até a V132 a tabela não tinha RLS: qualquer consulta do
-- crm_app enxergava e alterava todas as empresas. A partir daqui:
--
--   SELECT  empresa ativa (app.current_company_id)
--           | dono do CRM (app.is_platform_admin(), V132)
--           | empresas do usuário da sessão: users.company_id + memberships
--             ativas (login/gate de acesso e seletor de empresas, que leem a
--             empresa antes de o tenant ser definido)
--           | empresa do link público em uso (convite, formulário de anamnese,
--             link de cadastro), pelos mesmos GUCs de token das tabelas de link
--   INSERT  empresa ativa | dono do CRM | sessão identificada (onboarding: a
--           empresa ainda não existe, então não há tenant)
--   UPDATE  empresa ativa | dono do CRM
--   DELETE  dono do CRM
--
-- Rotinas sem usuário (jobs e seeders) listam as empresas por
-- app.all_company_ids() e leem cada uma já com o tenant dela. As checagens de
-- CNPJ/e-mail duplicado precisam enxergar todas as empresas e usam funções
-- próprias (só devolvem sim/não ou o id).
--
-- Todas as funções são SECURITY DEFINER (users, memberships e as tabelas de
-- link têm RLS FORCE) e só são executáveis pelo crm_app.

SET lock_timeout = '5s';

-- ---------------------------------------------------------------------------
-- 1. Funções auxiliares
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app.has_session_identity()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
    SELECT NULLIF(current_setting('app.current_keycloak_sub', TRUE), '') IS NOT NULL
        OR NULLIF(current_setting('app.current_identity_email', TRUE), '') IS NOT NULL;
$$;

-- Mesmo critério de identificação de membership_own_policy (sub ou e-mail).
CREATE OR REPLACE FUNCTION app.session_company_ids()
RETURNS SETOF UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    WITH me AS (
        SELECT u.id, u.company_id
        FROM users u
        WHERE u.keycloak_sub = NULLIF(current_setting('app.current_keycloak_sub', TRUE), '')
           OR u.email = NULLIF(current_setting('app.current_identity_email', TRUE), '')
    )
    SELECT me.company_id FROM me WHERE me.company_id IS NOT NULL
    UNION
    SELECT m.company_id
    FROM memberships m
    JOIN me ON me.id = m.user_id
    WHERE m.status = 'ACTIVE';
$$;

CREATE OR REPLACE FUNCTION app.token_company_ids()
RETURNS SETOF UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT i.company_id FROM invitations i
    WHERE i.token_hash = app.current_invitation_token_hash()
    UNION
    SELECT unnest(ARRAY[ci.company_id, ci.inviter_company_id]) FROM company_invitations ci
    WHERE ci.token_hash = app.current_invitation_token_hash()
    UNION
    SELECT f.company_id FROM patient_form_requests f
    WHERE f.token = app.current_form_token()
    UNION
    SELECT o.company_id FROM anamnesis_open_links o
    WHERE o.token = app.current_form_token()
    UNION
    SELECT r.company_id FROM patient_registration_links r
    WHERE r.token = app.current_registration_token();
$$;

CREATE OR REPLACE FUNCTION app.all_company_ids()
RETURNS SETOF UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT id FROM companies ORDER BY created_at, id;
$$;

CREATE OR REPLACE FUNCTION app.company_id_by_cnpj(p_cnpj TEXT)
RETURNS UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT id FROM companies WHERE cnpj = p_cnpj;
$$;

CREATE OR REPLACE FUNCTION app.company_email_exists(p_email TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (SELECT 1 FROM companies WHERE email = p_email);
$$;

REVOKE ALL ON FUNCTION app.session_company_ids() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.token_company_ids() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.all_company_ids() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.company_id_by_cnpj(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION app.company_email_exists(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.has_session_identity() TO crm_app;
GRANT EXECUTE ON FUNCTION app.session_company_ids() TO crm_app;
GRANT EXECUTE ON FUNCTION app.token_company_ids() TO crm_app;
GRANT EXECUTE ON FUNCTION app.all_company_ids() TO crm_app;
GRANT EXECUTE ON FUNCTION app.company_id_by_cnpj(TEXT) TO crm_app;
GRANT EXECUTE ON FUNCTION app.company_email_exists(TEXT) TO crm_app;

-- ---------------------------------------------------------------------------
-- 2. Policies
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS companies_select_policy ON companies;
DROP POLICY IF EXISTS companies_insert_policy ON companies;
DROP POLICY IF EXISTS companies_update_policy ON companies;
DROP POLICY IF EXISTS companies_delete_policy ON companies;

CREATE POLICY companies_select_policy ON companies
    FOR SELECT
    USING (
        id = app.current_tenant_id()
        OR app.is_platform_admin()
        OR id IN (SELECT app.session_company_ids())
        OR id IN (SELECT app.token_company_ids())
    );

CREATE POLICY companies_insert_policy ON companies
    FOR INSERT
    WITH CHECK (
        id = app.current_tenant_id()
        OR app.is_platform_admin()
        OR app.has_session_identity()
    );

CREATE POLICY companies_update_policy ON companies
    FOR UPDATE
    USING (id = app.current_tenant_id() OR app.is_platform_admin())
    WITH CHECK (id = app.current_tenant_id() OR app.is_platform_admin());

CREATE POLICY companies_delete_policy ON companies
    FOR DELETE
    USING (app.is_platform_admin());

ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;

RESET lock_timeout;
