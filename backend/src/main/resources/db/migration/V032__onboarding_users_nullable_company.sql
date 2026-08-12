-- V032__onboarding_users_nullable_company.sql
-- Sprint 8.3 - Onboarding
-- Habilita o self-service "crie sua empresa": o usuário pode ser provisionado
-- SEM empresa (company_id NULL) e criar a primeira empresa via onboarding.
--
-- Duas mudanças estruturais:
--   1) users.company_id deixa de ser NOT NULL (empresa ativa vira opcional —
--      "usuário sem empresa" passa a ser representável).
--   2) Nova policy RLS que permite o INSERT de uma linha PRÓPRIA com
--      company_id NULL (bootstrap por identidade), mantendo FORCE ROW LEVEL
--      SECURITY e o isolamento por tenant intactos.
--
-- A tabela companies é global (sem RLS); companies/memberships/roles seguem
-- as policies existentes (V019/V030). O fluxo de onboarding cria a empresa e,
-- via trigger membership_sync_active_company (V030), define users.company_id
-- quando a primeira membership ACTIVE (OWNER) for inserida.

-- ===========================================================================
-- 1. Relaxar users.company_id (empresa ativa torna-se opcional)
-- ===========================================================================
ALTER TABLE users ALTER COLUMN company_id DROP NOT NULL;

COMMENT ON COLUMN users.company_id IS
    'Empresa ativa (denormalizada). NULL = usuário sem empresa (onboarding pendente).';

-- ===========================================================================
-- 2. Policy RLS de INSERT para usuário sem empresa
-- ===========================================================================
-- Sob RLS FORCE e crm_app (NOBYPASSRLS), o INSERT em users exigia
-- company_id = app.current_tenant_id() (WITH CHECK herdado da policy
-- tenant_isolation_policy do V019). Para provisionar um usuário SEM empresa,
-- adiciona-se uma policy de INSERT que permite criar a PRÓPRIA linha com
-- company_id IS NULL, desde que a identidade coincida com o GUC de bootstrap
-- (keycloak_sub/e-mail do JWT autenticado) — mesmo padrão do V022/V024/V025.
-- Assim nenhum usuário consegue criar uma linha de OUTREM nem com tenant
-- arbitrário (as duas condições são simultâneas e obrigatórias).
CREATE POLICY identity_onboarding_insert_policy ON users
    FOR INSERT
    WITH CHECK (
        company_id IS NULL
        AND (
            keycloak_sub = NULLIF(current_setting('app.current_keycloak_sub', true), '')
            OR email = NULLIF(current_setting('app.current_identity_email', true), '')
        )
    );

-- ===========================================================================
-- 3. Trigger V030 permanece responsável por "elevar" company_id quando o
--    usuário ganha a primeira membership ACTIVE (criação de empresa no
--    onboarding). Nada a alterar aqui — apenas validação abaixo.
-- ===========================================================================
DO $$
DECLARE
    is_nullable BOOLEAN;
    is_forced BOOLEAN;
    insert_policy BOOLEAN;
    has_trigger BOOLEAN;
    null_count INTEGER;
BEGIN
    SELECT (ic.is_nullable = 'YES')
    FROM information_schema.columns ic
    WHERE ic.table_schema = 'public' AND ic.table_name = 'users' AND ic.column_name = 'company_id'
    INTO is_nullable;

    SELECT c.relforcerowsecurity INTO is_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'users';

    SELECT EXISTS (SELECT 1 FROM pg_policies
                   WHERE schemaname = 'public' AND tablename = 'users'
                     AND policyname = 'identity_onboarding_insert_policy'
                     AND cmd = 'INSERT') INTO insert_policy;

    SELECT EXISTS (SELECT 1 FROM pg_trigger
                   WHERE tgname = 'trg_membership_sync_active_company'
                     AND NOT tgisinternal) INTO has_trigger;

    SELECT COUNT(*) INTO null_count FROM users WHERE company_id IS NULL;

    IF is_nullable IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'V032: users.company_id deveria ser NULLABLE';
    END IF;
    IF NOT is_forced THEN
        RAISE EXCEPTION 'V032: users perdeu FORCE ROW LEVEL SECURITY';
    END IF;
    IF NOT insert_policy THEN
        RAISE EXCEPTION 'V032: policy identity_onboarding_insert_policy (INSERT) ausente';
    END IF;
    IF NOT has_trigger THEN
        RAISE EXCEPTION 'V032: trigger trg_membership_sync_active_company ausente';
    END IF;

    RAISE NOTICE 'V032: users.company_id NULLABLE ok (onboarding); % usuário(s) sem empresa', null_count;
END $$;
