-- V028__drop_token_join_policy_password_reset_tokens.sql
-- Remove a policy de ISOLAMENTO (join em users) da tabela password_reset_tokens.
--
-- Contexto: V020 criou `tenant_isolation_policy` em password_reset_tokens via
-- `EXISTS (SELECT 1 FROM users u WHERE u.id = password_reset_tokens.user_id AND
-- u.company_id = app.current_tenant_id())`. A Sprint 7.4 (V027) passou a acessar
-- o fluxo anonimo de reset por POSSE DO TOKEN (GUC app.current_reset_token),
-- criando uma policy reciproca em `users` (`password_reset_user_select_policy`)
-- que subconsulta password_reset_tokens pelo token.
--
-- RECURSAO: ao avaliar a policy de `users`, o subselect em password_reset_tokens
-- aciona a policy V020, cujo `EXISTS` volta a subconsultar `users` -> ciclo
-- (PostgreSQL: "infinite recursion detected in policy for relation users").
--
-- O acesso a password_reset_tokens e EXCLUSIVO do fluxo de reset (findByToken,
-- save pos-forgot/reset) sob posse do token (V027). A policy V020 (join por
-- tenant) e redundante e agora prejudicial. RLS FORCE permanece em
-- password_reset_tokens.

-- ===========================================================================
-- 1. Remover a policy de join por tenant em password_reset_tokens
-- ===========================================================================
DROP POLICY IF EXISTS tenant_isolation_policy ON password_reset_tokens;

-- ===========================================================================
-- 2. Verificacao: RLS FORCE mantido, policies V027 presentes
-- ===========================================================================
DO $$
DECLARE
    tokens_forced BOOLEAN;
    token_select BOOLEAN;
    token_consume BOOLEAN;
    join_removed BOOLEAN;
BEGIN
    SELECT c.relforcerowsecurity INTO tokens_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'password_reset_tokens';

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'password_reset_tokens' AND p.cmd = 'SELECT'
    ) INTO token_select;

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'password_reset_tokens' AND p.cmd = 'UPDATE'
    ) INTO token_consume;

    SELECT NOT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'password_reset_tokens'
        AND p.policyname = 'tenant_isolation_policy'
    ) INTO join_removed;

    IF NOT tokens_forced THEN
        RAISE EXCEPTION 'V028: password_reset_tokens perdeu FORCE ROW LEVEL SECURITY';
    END IF;
    IF NOT token_select THEN
        RAISE EXCEPTION 'V028: policy de SELECT ausente em password_reset_tokens';
    END IF;
    IF NOT token_consume THEN
        RAISE EXCEPTION 'V028: policy de UPDATE ausente em password_reset_tokens';
    END IF;
    IF NOT join_removed THEN
        RAISE EXCEPTION 'V028: policy tenant_isolation_policy ainda presente em password_reset_tokens';
    END IF;

    RAISE NOTICE 'V028: join policy removida de password_reset_tokens (RLS FORCE mantido, V027 token gate preservado)';
END $$;