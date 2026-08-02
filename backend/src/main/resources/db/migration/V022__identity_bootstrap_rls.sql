-- V022__identity_bootstrap_rls.sql
-- Bootstrap de identidade sob RLS FORCE: permite que o app leia a PRÓPRIA linha
-- em `users` via `app.current_keycloak_sub` (GUC definido pelo datasource a
-- partir do `sub` do JWT) ANTES de o `company_id` ser conhecido.
--
-- Contexto: na autenticação, a resolução do CurrentUser consulta `users` por
-- `keycloak_sub` antes de o TenantFilter definir `app.current_company_id`
-- (chicken-and-egg). Com RLS FORCE e crm_app (NOBYPASSRLS), a linha ficava
-- invisível e o login retornava 401 (ex.: "Role padrão não encontrada no banco").
--
-- A política adiciona um OR sobre o isolamento por tenant: uma linha é visível
-- se o tenant bate OU se o `keycloak_sub` da linha é o sub autenticado.
-- RLS FORCE permanece em `users` e nas demais 17 tabelas tenant-scoped.

CREATE POLICY identity_bootstrap_policy ON users
    FOR SELECT
    USING (keycloak_sub = NULLIF(current_setting('app.current_keycloak_sub', true), ''));

-- Verificação: policy criada e users ainda FORCE RLS
DO $$
DECLARE
    has_bootstrap BOOLEAN;
    is_forced BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM pg_policies p
        WHERE p.schemaname = 'public'
        AND p.tablename = 'users'
        AND p.policyname = 'identity_bootstrap_policy'
    ) INTO has_bootstrap;

    SELECT c.relforcerowsecurity INTO is_forced
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'users';

    IF NOT has_bootstrap THEN
        RAISE EXCEPTION 'V022: policy identity_bootstrap_policy ausente em users';
    END IF;
    IF NOT is_forced THEN
        RAISE EXCEPTION 'V022: users perdeu FORCE ROW LEVEL SECURITY';
    END IF;

    RAISE NOTICE 'V022: bootstrap por keycloak_sub aplicado em users (RLS FORCE mantido)';
END $$;
