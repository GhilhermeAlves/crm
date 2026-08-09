-- V030__memberships.sql
-- Sprint 8.2 - Membership
-- Fonte de verdade da relacao usuario <-> empresa (1:N). users.company_id
-- permanece NOT NULL como "empresa ativa" denormalizada, com consistencia via
-- trigger (app.membership_sync_active_company).
--
-- RLS: FORCE em memberships. Duas camadas de politica:
--   * membership_own_policy (SELECT): o usuario enxerga APENAS as proprias
--     memberships (bootstrap por keycloak_sub/e-mail, padrao V022/V024).
--   * membership_tenant_policy (ALL): dentro do contexto de uma empresa, as
--     memberships daquele tenant sao visiveis/mutaveis (lista de membros e
--     gestao por ADMIN).
--
-- NOTA sobre backfill: sob RLS FORCE e usuário de app NOBYPASSRLS (crm_app),
-- o SELECT sobre users/user_roles retorna 0 linhas (sem GUC de tenant durante
-- a migracao). O backfill abaixo é, portanto, BEST-EFFORT (funciona apenas
-- quando a migracao roda como superuser). A sincronizacao garantida de todos
-- os usuarios existentes é feita em tempo de startup pelo MembershipDataSeeder
-- (application-level, RLS-safe, padrao RoleDataSeeder).

-- ===========================================================================
-- 1. Tabela memberships
-- ===========================================================================
CREATE TABLE memberships (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    role       VARCHAR(50) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'PENDING', 'REMOVED')),
    invited_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    joined_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Uma unica membership ativa/pendente por (usuario, empresa). REMOVED fica fora
-- do indice parcial para permitir re-adesao futura (8.5) sem violar unicidade.
CREATE UNIQUE INDEX idx_memberships_active_user_company
    ON memberships (user_id, company_id)
    WHERE status IN ('ACTIVE', 'PENDING');

CREATE INDEX idx_memberships_company_id ON memberships(company_id);
CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_user_status ON memberships(user_id, status);

-- ===========================================================================
-- 2. Backfill (best-effort) a partir de users.company_id + user_roles
-- ===========================================================================
-- Papel vigente: prioridade SUPER_ADMIN > OWNER > ADMIN > MANAGER > AGENT >
-- VIEWER. Usuarios sem role no banco recebem 'AGENT' (todos os provisionados
-- possuem ao menos uma role via assignDefaultRole).
INSERT INTO memberships (user_id, company_id, role, status, joined_at, created_at)
SELECT u.id,
       u.company_id,
       COALESCE(
           (SELECT r.name
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = u.id
              AND ur.company_id = u.company_id
            ORDER BY CASE r.name
                         WHEN 'SUPER_ADMIN' THEN 6
                         WHEN 'OWNER'      THEN 5
                         WHEN 'ADMIN'      THEN 4
                         WHEN 'MANAGER'    THEN 3
                         WHEN 'AGENT'      THEN 2
                         ELSE 1
                     END DESC
            LIMIT 1),
           'AGENT'),
       'ACTIVE',
       u.created_at,
       NOW()
FROM users u
ON CONFLICT DO NOTHING;

-- ===========================================================================
-- 3. Permissoes de membership + atribuicao a ADMIN
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('membership:view',   'View company members',   'identity', 'membership', 'view'),
    ('membership:manage', 'Manage company members', 'identity', 'membership', 'manage')
ON CONFLICT (name) DO NOTHING;

-- ADMIN (em todas as empresas) gerencia e visualiza membros.
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('membership:view', 'membership:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ===========================================================================
-- 4. RLS FORCE + policies
-- ===========================================================================
ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE memberships FORCE ROW LEVEL SECURITY;

-- Membro enxerga apenas as PRÓPRIAS memberships (cross-company p/ /me/memberships).
CREATE POLICY membership_own_policy ON memberships
    FOR SELECT
    USING (
        user_id IN (
            SELECT id FROM users
            WHERE keycloak_sub = NULLIF(current_setting('app.current_keycloak_sub', true), '')
               OR email = NULLIF(current_setting('app.current_identity_email', true), '')
        )
    );

-- Isolamento por tenant: dentro do contexto da empresa, membros/gestores operam
-- as memberships do próprio tenant.
CREATE POLICY membership_tenant_policy ON memberships
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- 5. Trigger de consistencia da "empresa ativa" (users.company_id)
-- ===========================================================================
-- Define users.company_id somente quando a membership ativa inserida/reativada
-- é a ÚNICA membership ativa do usuário (nunca rouba a empresa ativa ao adicionar
-- uma segunda membership — a troca de empresa ativa é responsabilidade da 8.4).
-- A remoção NÃO move company_id para outra empresa (decisão RLS-safe: a troca
-- cross-tenant exige o switcher da 8.4); membro removido perde acesso via
-- remoção de user_roles + gate de membership ativa na resolução do CurrentUser.
CREATE OR REPLACE FUNCTION app.membership_sync_active_company()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        UPDATE users
        SET company_id = NEW.company_id, updated_at = NOW()
        WHERE id = NEW.user_id
          AND NOT EXISTS (
              SELECT 1 FROM memberships m
              WHERE m.user_id = NEW.user_id AND m.status = 'ACTIVE'
          );
    END IF;
    RETURN COALESCE(NEW, OLD);
END $$;

CREATE TRIGGER trg_membership_sync_active_company
    AFTER INSERT OR UPDATE OF status ON memberships
    FOR EACH ROW
    EXECUTE FUNCTION app.membership_sync_active_company();

-- ===========================================================================
-- 6. Verificacao estrutural (pg_catalog, imune a RLS)
-- ===========================================================================
DO $$
DECLARE
    has_rls BOOLEAN;
    is_forced BOOLEAN;
    own_policy BOOLEAN;
    tenant_policy BOOLEAN;
    has_trigger BOOLEAN;
    view_perm BOOLEAN;
    manage_perm BOOLEAN;
    admin_grants INTEGER;
BEGIN
    SELECT c.relrowsecurity INTO has_rls
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'memberships';

    SELECT c.relforcerowsecurity INTO is_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'memberships';

    SELECT EXISTS (SELECT 1 FROM pg_policies
                   WHERE schemaname = 'public' AND tablename = 'memberships'
                     AND policyname = 'membership_own_policy') INTO own_policy;

    SELECT EXISTS (SELECT 1 FROM pg_policies
                   WHERE schemaname = 'public' AND tablename = 'memberships'
                     AND policyname = 'membership_tenant_policy') INTO tenant_policy;

    SELECT EXISTS (SELECT 1 FROM pg_trigger
                   WHERE tgname = 'trg_membership_sync_active_company'
                     AND NOT tgisinternal) INTO has_trigger;

    SELECT EXISTS (SELECT 1 FROM permissions WHERE name = 'membership:view') INTO view_perm;
    SELECT EXISTS (SELECT 1 FROM permissions WHERE name = 'membership:manage') INTO manage_perm;

    SELECT COUNT(*) INTO admin_grants
    FROM role_permissions rp
    JOIN roles r ON r.id = rp.role_id
    JOIN permissions p ON p.id = rp.permission_id
    WHERE r.name = 'ADMIN' AND p.name IN ('membership:view', 'membership:manage');

    IF NOT has_rls THEN RAISE EXCEPTION 'V030: RLS ausente em memberships'; END IF;
    IF NOT is_forced THEN RAISE EXCEPTION 'V030: memberships sem FORCE ROW LEVEL SECURITY'; END IF;
    IF NOT own_policy THEN RAISE EXCEPTION 'V030: policy membership_own_policy ausente'; END IF;
    IF NOT tenant_policy THEN RAISE EXCEPTION 'V030: policy membership_tenant_policy ausente'; END IF;
    IF NOT has_trigger THEN RAISE EXCEPTION 'V030: trigger trg_membership_sync_active_company ausente'; END IF;
    IF NOT view_perm THEN RAISE EXCEPTION 'V030: permissao membership:view ausente'; END IF;
    IF NOT manage_perm THEN RAISE EXCEPTION 'V030: permissao membership:manage ausente'; END IF;
    IF admin_grants < 2 THEN RAISE EXCEPTION 'V030: ADMIN sem grants de membership (%)', admin_grants; END IF;

    RAISE NOTICE 'V030: memberships criado (RLS FORCE, policies, trigger e permissoes ok)';
END $$;
