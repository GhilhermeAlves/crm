-- V026__identity_phone_bootstrap_rls.sql
-- Bootstrap de identidade por TELEFONE (Sprint 7.3) sob RLS FORCE.
--
-- Contexto: o fluxo phone/OTP é anônimo (verify-otp acontece ANTES de o usuário
-- possuir um JWT/company_id). Após validar o OTP (prova de POSSE do telefone),
-- o app precisa LOCALIZAR a própria conta em `users` por telefone — sem isto o
-- RLS FORCE tornaria a linha invisível e o verify-otp sempre retornaria
-- USER_NOT_FOUND.
--
-- Solução (mesmo padrão V024/V025, nunca bypass de RLS): as policies só
-- habilitam SELECT/UPDATE quando o GUC `app.current_identity_phone` é definido
-- pelo datasource EXCLUSIVAMENTE após a OTP ser validada no código (a prova de
-- posse é a própria OTP; a política apenas habilita a operação). `users.phone`
-- não é UNIQUE global — mas a OTP é gerada POR telefone (otp_codes.phone_e164),
-- então o app só consegue ler a conta para o telefone cujo código foi validado.
--
-- RLS FORCE permanece em `users` e nas demais tabelas tenant-scoped.

CREATE POLICY identity_phone_bootstrap_policy ON users
    FOR SELECT
    USING (phone = NULLIF(current_setting('app.current_identity_phone', true), ''));

CREATE POLICY identity_phone_link_policy ON users
    FOR UPDATE
    USING (phone = NULLIF(current_setting('app.current_identity_phone', true), ''))
    WITH CHECK (phone = NULLIF(current_setting('app.current_identity_phone', true), ''));

-- Verificação: políticas criadas e users ainda FORCE RLS
DO $$
DECLARE
    has_select BOOLEAN;
    has_update BOOLEAN;
    is_forced BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'users'
        AND p.policyname = 'identity_phone_bootstrap_policy' AND p.cmd = 'SELECT'
    ) INTO has_select;

    SELECT EXISTS (
        SELECT 1 FROM pg_policies p
        WHERE p.tablename = 'users'
        AND p.policyname = 'identity_phone_link_policy' AND p.cmd = 'UPDATE'
    ) INTO has_update;

    SELECT c.relforcerowsecurity INTO is_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'users';

    IF NOT has_select THEN
        RAISE EXCEPTION 'V026: policy identity_phone_bootstrap_policy (SELECT) ausente em users';
    END IF;
    IF NOT has_update THEN
        RAISE EXCEPTION 'V026: policy identity_phone_link_policy (UPDATE) ausente em users';
    END IF;
    IF NOT is_forced THEN
        RAISE EXCEPTION 'V026: users perdeu FORCE ROW LEVEL SECURITY';
    END IF;

    RAISE NOTICE 'V026: bootstrap por telefone (SELECT/UPDATE) aplicado em users (RLS FORCE mantido)';
END $$;