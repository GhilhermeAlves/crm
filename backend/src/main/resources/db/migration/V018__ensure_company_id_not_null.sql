-- V018__ensure_company_id_not_null.sql
-- Garante que todas as tabelas tenant-scoped tenham company_id NOT NULL
-- DEVE rodar ANTES de habilitar RLS (V019)

-- ===========================================================================
-- 1. Garantir que existe pelo menos uma empresa na tabela companies
-- ===========================================================================
-- Se não existir nenhuma empresa, cria uma empresa default do sistema
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM companies LIMIT 1) THEN
        INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
            address_zip_code, address_street, address_number, address_neighborhood,
            address_city, address_state, address_country, plan, status, max_users, max_storage_mb)
        VALUES (
            '00000000-0000-0000-0000-000000000001',
            'Empresa Default',
            'Default',
            '00000000000000',
            'admin@default.com',
            '00000000000',
            '00000-000',
            'Rua Default',
            '0',
            'Centro',
            'Sao Paulo',
            'SP',
            'Brasil',
            'STARTER',
            'ACTIVE',
            100,
            10240
        ) ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- ===========================================================================
-- 2. Atribuir company_id default para registros órfãos (company_id IS NULL)
-- ===========================================================================

-- users
UPDATE users SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- roles
UPDATE roles SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- user_roles
UPDATE user_roles SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- audit_logs
UPDATE audit_logs SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- pipelines
UPDATE pipelines SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- stages
UPDATE stages SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- opportunities
UPDATE opportunities SET company_id = '00000000-0000-0000-0000-000000000001'
WHERE company_id IS NULL;

-- ===========================================================================
-- 3. Migrar roles default (UUID zero) para empresa real
-- ===========================================================================
-- Roles default do V002 usam company_id = '00000000-0000-0000-0000-000000000000'
-- Migrar para a primeira empresa ativa existente (não para um UUID fixo,
-- pois em produção a empresa real pode ter outro id)
DO $$
DECLARE
    target_company UUID;
BEGIN
    SELECT id INTO target_company
    FROM companies
    WHERE status = 'ACTIVE'
    ORDER BY created_at, id
    LIMIT 1;

    IF target_company IS NULL THEN
        SELECT id INTO target_company
        FROM companies
        ORDER BY created_at, id
        LIMIT 1;
    END IF;

    IF target_company IS NOT NULL THEN
        UPDATE roles SET company_id = target_company
        WHERE company_id = '00000000-0000-0000-0000-000000000000';

        UPDATE user_roles SET company_id = target_company
        WHERE company_id = '00000000-0000-0000-0000-000000000000';

        RAISE NOTICE 'V018: roles globais migradas para empresa %', target_company;
    END IF;
END $$;

-- ===========================================================================
-- 4. Adicionar NOT NULL constraints onde ainda não existe
-- ===========================================================================

-- users.company_id
DO $$
BEGIN
    ALTER TABLE users ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'users.company_id already NOT NULL or constraint error';
END $$;

-- roles.company_id
DO $$
BEGIN
    ALTER TABLE roles ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'roles.company_id already NOT NULL or constraint error';
END $$;

-- user_roles.company_id
DO $$
BEGIN
    ALTER TABLE user_roles ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'user_roles.company_id already NOT NULL or constraint error';
END $$;

-- audit_logs.company_id
DO $$
BEGIN
    ALTER TABLE audit_logs ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'audit_logs.company_id already NOT NULL or constraint error';
END $$;

-- pipelines.company_id
DO $$
BEGIN
    ALTER TABLE pipelines ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'pipelines.company_id already NOT NULL or constraint error';
END $$;

-- stages.company_id
DO $$
BEGIN
    ALTER TABLE stages ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'stages.company_id already NOT NULL or constraint error';
END $$;

-- opportunities.company_id
DO $$
BEGIN
    ALTER TABLE opportunities ALTER COLUMN company_id SET NOT NULL;
EXCEPTION WHEN others THEN
    RAISE NOTICE 'opportunities.company_id already NOT NULL or constraint error';
END $$;

-- ===========================================================================
-- 5. Verificação final
-- ===========================================================================
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM users WHERE company_id IS NULL;
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'Ainda existem % users sem company_id', orphan_count;
    END IF;

    SELECT COUNT(*) INTO orphan_count
    FROM roles WHERE company_id IS NULL;
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'Ainda existem % roles sem company_id', orphan_count;
    END IF;

    RAISE NOTICE 'V018: company_id NOT NULL verificado com sucesso em todas as tabelas';
END $$;
