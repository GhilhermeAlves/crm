-- V023__add_crm_enabled_to_users.sql
-- CRM Access explícito (Sprint 6): separa "quem se autenticou no Keycloak" de
-- "quem pode entrar no CRM".
--
-- users.crm_enabled  = acesso explícito à aplicação CRM (não é RBAC, não é
--                       substituição de is_active). Novo usuário (default)
--                       fica SEM acesso até concessão explícita.
--
-- Backfill idempotente: usuários EXISTENTES e válidos (is_active = true e não
-- soft-deleted) mantêm acesso → crm_enabled = true. Usuários inativos,
-- deletados ou novos continuam com crm_enabled = false. Nada é concedido
-- indiscriminadamente; a empresa continua sendo um gate separado
-- (companies.status) na camada de aplicação.

ALTER TABLE users ADD COLUMN IF NOT EXISTS crm_enabled BOOLEAN NOT NULL DEFAULT false;

-- Backfill (idempotente): preserva o acesso legítimo atual sem bloquear
-- ninguém no deploy. Atualiza somente registros ainda sem acesso.
UPDATE users
SET crm_enabled = true
WHERE is_active = true
  AND deleted_at IS NULL
  AND crm_enabled = false;

CREATE INDEX IF NOT EXISTS idx_users_crm_enabled ON users(crm_enabled);

-- Verificação final: coluna existe, NOT NULL, default false e backfill aplicado.
DO $$
DECLARE
    has_column BOOLEAN;
    is_not_null BOOLEAN;
    default_value TEXT;
    pending_count INTEGER;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
        AND table_name = 'users'
        AND column_name = 'crm_enabled'
    ) INTO has_column;

    SELECT is_nullable = 'NO' INTO is_not_null
    FROM information_schema.columns
    WHERE table_schema = 'public'
    AND table_name = 'users'
    AND column_name = 'crm_enabled';

    SELECT column_default INTO default_value
    FROM information_schema.columns
    WHERE table_schema = 'public'
    AND table_name = 'users'
    AND column_name = 'crm_enabled';

    SELECT COUNT(*) INTO pending_count
    FROM users
    WHERE is_active = true
      AND deleted_at IS NULL
      AND crm_enabled = false;

    IF NOT has_column OR NOT is_not_null THEN
        RAISE EXCEPTION 'V023: users.crm_enabled ausente ou não-NOT NULL';
    END IF;
    IF default_value IS DISTINCT FROM 'false' THEN
        RAISE EXCEPTION 'V023: default de users.crm_enabled não é false (%)', default_value;
    END IF;
    IF pending_count > 0 THEN
        RAISE EXCEPTION 'V023: % usuários válidos sem crm_enabled após backfill', pending_count;
    END IF;

    RAISE NOTICE 'V023: crm_enabled aplicado em users (default false) com backfill dos usuários válidos';
END $$;
