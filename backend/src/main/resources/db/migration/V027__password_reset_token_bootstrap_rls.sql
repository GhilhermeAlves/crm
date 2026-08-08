-- V027__password_reset_token_bootstrap_rls.sql
-- Bootstrap do fluxo ANÔNIMO de redefinição de senha (Sprint 7.4) sob RLS FORCE.
--
-- Contexto: forgot-password e reset-password são endpoints públicos (sem JWT e
-- sem company_id). Com RLS FORCE em `users` (V019) e `password_reset_tokens`
-- (V020 — isolado via join com users.company_id), o app crm_app não consegue:
--   (a) FORGOT — localizar o usuário por e-mail (resolvido por V025 via
--                `app.current_identity_email`) e INSERIR o novo token;
--   (b) RESET  — ler o token e o usuário dono, e marcar o token como usado.
--
-- Solução (mesmo padrão V022/V024/V025/V026 — nunca bypass de RLS): as policies
-- abaixo só habilitam SELECT/UPDATE quando o GUC `app.current_reset_token` é
-- definido pelo datasource EXCLUSIVAMENTE no escopo da requisição de reset — o
-- token é o segredo de posse. Assim o app consegue:
--   SELECT  — ler a linha do token e a conta dona via token (bootstrap);
--   UPDATE  — marcar o token como usado e, no fallback legado, trocar a senha
--             local; o gate da operação é o próprio token + validações do app.
--
-- RLS FORCE permanece em `users` e `password_reset_tokens`.

-- ===========================================================================
-- 1. password_reset_tokens: bootstrap por token (SELECT / INSERT / UPDATE)
-- ===========================================================================
CREATE POLICY password_reset_token_select_policy ON password_reset_tokens
    FOR SELECT
    USING (token = NULLIF(current_setting('app.current_reset_token', true), ''));

CREATE POLICY password_reset_token_insert_policy ON password_reset_tokens
    FOR INSERT
    WITH CHECK (token = NULLIF(current_setting('app.current_reset_token', true), ''));

CREATE POLICY password_reset_token_consume_policy ON password_reset_tokens
    FOR UPDATE
    USING (token = NULLIF(current_setting('app.current_reset_token', true), ''))
    WITH CHECK (token = NULLIF(current_setting('app.current_reset_token', true), ''));

-- ===========================================================================
-- 2. users: localizar e (fallback legado) atualizar a conta dona do token
-- ===========================================================================
CREATE POLICY password_reset_user_select_policy ON users
    FOR SELECT
    USING (id IN (
        SELECT user_id FROM password_reset_tokens
        WHERE token = NULLIF(current_setting('app.current_reset_token', true), '')
    ));

CREATE POLICY password_reset_user_update_policy ON users
    FOR UPDATE
    USING (id IN (
        SELECT user_id FROM password_reset_tokens
        WHERE token = NULLIF(current_setting('app.current_reset_token', true), '')
    ))
    WITH CHECK (id IN (
        SELECT user_id FROM password_reset_tokens
        WHERE token = NULLIF(current_setting('app.current_reset_token', true), '')
    ));

-- ===========================================================================
-- 3. Verificação: policies criadas e RLS FORCE mantido
-- ===========================================================================
DO $$
DECLARE
    token_select BOOLEAN;
    token_insert BOOLEAN;
    token_consume BOOLEAN;
    user_select BOOLEAN;
    user_update BOOLEAN;
    tokens_forced BOOLEAN;
    users_forced BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'password_reset_tokens'
        AND p.policyname = 'password_reset_token_select_policy' AND p.cmd = 'SELECT'
    ) INTO token_select;

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'password_reset_tokens'
        AND p.policyname = 'password_reset_token_insert_policy' AND p.cmd = 'INSERT'
    ) INTO token_insert;

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'password_reset_tokens'
        AND p.policyname = 'password_reset_token_consume_policy' AND p.cmd = 'UPDATE'
    ) INTO token_consume;

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'users'
        AND p.policyname = 'password_reset_user_select_policy' AND p.cmd = 'SELECT'
    ) INTO user_select;

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'users'
        AND p.policyname = 'password_reset_user_update_policy' AND p.cmd = 'UPDATE'
    ) INTO user_update;

    SELECT c.relforcerowsecurity INTO tokens_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'password_reset_tokens';

    SELECT c.relforcerowsecurity INTO users_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'users';

    IF NOT token_select OR NOT token_insert OR NOT token_consume THEN
        RAISE EXCEPTION 'V027: policies de token (SELECT/INSERT/UPDATE) ausentes em password_reset_tokens';
    END IF;
    IF NOT user_select OR NOT user_update THEN
        RAISE EXCEPTION 'V027: policies de usuário (SELECT/UPDATE) ausentes em users';
    END IF;
    IF NOT tokens_forced OR NOT users_forced THEN
        RAISE EXCEPTION 'V027: RLS FORCE perdido em password_reset_tokens/users';
    END IF;

    RAISE NOTICE 'V027: bootstrap de reset de senha por token aplicado (RLS FORCE mantido)';
END $$;
