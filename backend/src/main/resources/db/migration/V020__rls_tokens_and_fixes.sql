-- V020__rls_tokens_and_fixes.sql
-- Adiciona RLS em refresh_tokens e password_reset_tokens via join com users
-- Corrige gap de segurança identificado na auditoria

-- ===========================================================================
-- 1. RLS para refresh_tokens (via users.company_id)
-- ===========================================================================
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON refresh_tokens
    USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = refresh_tokens.user_id
            AND u.company_id = app.current_tenant_id()
        )
    );

-- ===========================================================================
-- 2. RLS para password_reset_tokens (via users.company_id)
-- ===========================================================================
ALTER TABLE password_reset_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_tokens FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON password_reset_tokens
    USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = password_reset_tokens.user_id
            AND u.company_id = app.current_tenant_id()
        )
    );

-- ===========================================================================
-- 3. Verificação final
-- ===========================================================================
DO $$
DECLARE
    rls_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO rls_count
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE c.relkind = 'r'
    AND c.relrowsecurity = true
    AND n.nspname = 'public'
    AND c.relname IN (
        'users', 'roles', 'user_roles', 'audit_logs',
        'company_settings', 'subscriptions', 'pipelines',
        'stages', 'opportunities', 'opportunity_history',
        'refresh_tokens', 'password_reset_tokens'
    );

    IF rls_count != 12 THEN
        RAISE EXCEPTION 'RLS habilitado em % tabelas (esperado 12)', rls_count;
    END IF;

    RAISE NOTICE 'V020: RLS habilitado com sucesso em 12 tabelas tenant-scoped';
END $$;
