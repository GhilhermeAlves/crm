-- V067__grant_user_permissions_table.sql
-- Sprint 20 Fase 2 (hotfix): a tabela user_permissions (V065) foi criada
-- APÓS o loop de grants da V062 — mesmo padrão do pitfall V034/V046.
-- O auth-service (crm_app) lê user_permissions na resolução do current-user;
-- sem o GRANT o login OIDC falha com "permission denied for table
-- user_permissions" (500 no callback).
--
-- Reaplica o loop idempotente de grants CRUD para crm_app.
-- GRANT não afeta RLS: crm_app continua NOBYPASSRLS.

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT IN ('flyway_schema_history', 'permissions')
        ORDER BY tablename
    LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO crm_app', t);
    END LOOP;
END $$;
