-- V024__identity_email_bootstrap_rls.sql
-- Bootstrap de identidade por E-MAIL sob RLS FORCE (Sprint 7.2 — Account Linking).
--
-- Contexto: para vincular (link) uma identidade Google a uma conta local
-- existente, o backend precisa LOCALIZAR a conta por e-mail ANTES de conhecer o
-- company_id (o RLS FORCE em `users` só permite ver a própria linha por
-- `keycloak_sub` — V022 — ou pelo tenant). Sem isto, o Caso B não é detectável
-- e o Caso C criaria contas duplicadas por e-mail.
--
-- A solução ESTENDE o padrão V022 (nunca bypass de RLS): o GUC
-- `app.current_identity_email` é definido pelo datasource EXCLUSIVAMENTE a
-- partir do claim `email` do JWT autenticado (fonte confiável). Assim o app
-- consegue:
--   SELECT — ler a PRÓPRIA linha por e-mail (bootstrap de identidade);
--   UPDATE — vincular o `keycloak_sub` à própria linha por e-mail, somente
--           após o app verificar a senha da conta local no código (o gate é
--           a senha + email_verified; a política apenas habilita a operação).
--
-- Como `users.email` é UNIQUE global (V002), o e-mail identifica uma única
-- linha — o GUC nunca expõe múltiplas contas.
--
-- RLS FORCE permanece em `users` e nas demais tabelas tenant-scoped.

CREATE POLICY identity_email_bootstrap_policy ON users
    FOR SELECT
    USING (email = NULLIF(current_setting('app.current_identity_email', true), ''));

CREATE POLICY identity_email_link_policy ON users
    FOR UPDATE
    USING (email = NULLIF(current_setting('app.current_identity_email', true), ''))
    WITH CHECK (email = NULLIF(current_setting('app.current_identity_email', true), ''));

-- Verificação: políticas criadas e users ainda FORCE RLS
DO $$
DECLARE
    has_select BOOLEAN;
    has_update BOOLEAN;
    is_forced BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM pg_policies p
        WHERE p.schemaname = 'public'
        AND p.tablename = 'users'
        AND p.policyname = 'identity_email_bootstrap_policy'
        AND p.cmd = 'SELECT'
    ) INTO has_select;

    SELECT EXISTS (
        SELECT 1
        FROM pg_policies p
        WHERE p.schemaname = 'public'
        AND p.tablename = 'users'
        AND p.policyname = 'identity_email_link_policy'
        AND p.cmd = 'UPDATE'
    ) INTO has_update;

    SELECT c.relforcerowsecurity INTO is_forced
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'users';

    IF NOT has_select THEN
        RAISE EXCEPTION 'V024: policy identity_email_bootstrap_policy (SELECT) ausente em users';
    END IF;
    IF NOT has_update THEN
        RAISE EXCEPTION 'V024: policy identity_email_link_policy (UPDATE) ausente em users';
    END IF;
    IF NOT is_forced THEN
        RAISE EXCEPTION 'V024: users perdeu FORCE ROW LEVEL SECURITY';
    END IF;

    RAISE NOTICE 'V024: bootstrap por e-mail (SELECT/UPDATE) aplicado em users (RLS FORCE mantido)';
END $$;
