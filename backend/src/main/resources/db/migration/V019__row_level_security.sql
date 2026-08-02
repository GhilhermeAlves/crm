-- V019__row_level_security.sql
-- Habilita Row Level Security em todas as tabelas tenant-scoped
-- REQUER: V018 (company_id NOT NULL) e V013 (app.current_tenant_id())

-- ===========================================================================
-- 1. Função auxiliar para verificar se o usuário é SUPER_ADMIN
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.is_super_admin()
RETURNS BOOLEAN LANGUAGE SQL STABLE AS $$
    SELECT EXISTS (
        SELECT 1
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.id
        WHERE r.name = 'SUPER_ADMIN'
        AND r.company_id = app.current_tenant_id()
    )
$$;

-- ===========================================================================
-- 2. Habilitar RLS nas tabelas tenant-scoped
-- ===========================================================================

-- users
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- roles
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;

-- user_roles
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles FORCE ROW LEVEL SECURITY;

-- audit_logs
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;

-- company_settings
ALTER TABLE company_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_settings FORCE ROW LEVEL SECURITY;

-- subscriptions
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions FORCE ROW LEVEL SECURITY;

-- pipelines
ALTER TABLE pipelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE pipelines FORCE ROW LEVEL SECURITY;

-- stages
ALTER TABLE stages ENABLE ROW LEVEL SECURITY;
ALTER TABLE stages FORCE ROW LEVEL SECURITY;

-- opportunities
ALTER TABLE opportunities ENABLE ROW LEVEL SECURITY;
ALTER TABLE opportunities FORCE ROW LEVEL SECURITY;

-- opportunity_history (via opportunity company_id)
ALTER TABLE opportunity_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE opportunity_history FORCE ROW LEVEL SECURITY;

-- ===========================================================================
-- 3. Criar policies de isolamento por tenant
-- ===========================================================================

-- policy_users: filtra por company_id
CREATE POLICY tenant_isolation_policy ON users
    USING (company_id = app.current_tenant_id());

-- policy_roles: filtra por company_id
CREATE POLICY tenant_isolation_policy ON roles
    USING (company_id = app.current_tenant_id());

-- policy_user_roles: filtra por company_id
CREATE POLICY tenant_isolation_policy ON user_roles
    USING (company_id = app.current_tenant_id());

-- policy_audit_logs: filtra por company_id
CREATE POLICY tenant_isolation_policy ON audit_logs
    USING (company_id = app.current_tenant_id());

-- policy_company_settings: filtra por company_id
CREATE POLICY tenant_isolation_policy ON company_settings
    USING (company_id = app.current_tenant_id());

-- policy_subscriptions: filtra por company_id
CREATE POLICY tenant_isolation_policy ON subscriptions
    USING (company_id = app.current_tenant_id());

-- policy_pipelines: filtra por company_id
CREATE POLICY tenant_isolation_policy ON pipelines
    USING (company_id = app.current_tenant_id());

-- policy_stages: filtra por company_id
CREATE POLICY tenant_isolation_policy ON stages
    USING (company_id = app.current_tenant_id());

-- policy_opportunities: filtra por company_id
CREATE POLICY tenant_isolation_policy ON opportunities
    USING (company_id = app.current_tenant_id());

-- policy_opportunity_history: via opportunity.company_id (join)
CREATE POLICY tenant_isolation_policy ON opportunity_history
    USING (
        EXISTS (
            SELECT 1 FROM opportunities o
            WHERE o.id = opportunity_history.opportunity_id
            AND o.company_id = app.current_tenant_id()
        )
    );

-- ===========================================================================
-- 4. Tabelas GLOBAIS (sem RLS) - Companies, Permissions, etc.
-- ===========================================================================
-- companies: NÃO habilitar RLS - é a tabela de tenants, acessível globalmente
-- permissions: NÃO habilitar RLS - permissões são globais
-- role_permissions: NÃO habilitar RLS - via role (tenant-scoped)
-- refresh_tokens: NÃO habilitar RLS - via user (tenant-scoped)
-- password_reset_tokens: NÃO habilitar RLS - via user (tenant-scoped)

-- ===========================================================================
-- 5. Verificação final
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
        'stages', 'opportunities', 'opportunity_history'
    );

    IF rls_count != 10 THEN
        RAISE EXCEPTION 'RLS habilitado em % tabelas (esperado 10)', rls_count;
    END IF;

    RAISE NOTICE 'V019: RLS habilitado com sucesso em 10 tabelas tenant-scoped';
END $$;
