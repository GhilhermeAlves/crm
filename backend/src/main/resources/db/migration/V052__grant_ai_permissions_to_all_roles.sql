-- V052__grant_ai_permissions_to_all_roles.sql
-- AI (Sprints AI-01..AI-06): garante que TODOS os papeis de TODAS as empresas
-- recebam as permissoes do assistente de IA (ai:chat / ai:suggest), definidas
-- em V049/V050.
--
-- Motivo: o vinculo papel -> permissao e aplicado em startup pelo
-- RoleSeedService, mas papeis ja existentes ficaram sem ai:* (em especial o
-- SUPER_ADMIN, cujo wildcard '*' era um no-op e nao concedia nenhuma
-- permissao). Sem esses vinculos, as rotas /api/v1/ai/* respondem 403 para
-- todos os usuarios.
--
-- RLS: role_permissions NAO possui RLS, mas roles possui RLS FORCE. Sob o
-- usuario de app (crm_app, NOBYPASSRLS) um SELECT sobre roles retorna 0 linhas
-- sem o GUC de tenant. Por isso iteramos as empresas definindo
-- app.current_company_id por empresa (mesma politica do app/seeders).
--
-- Idempotente: UNIQUE(role_id, permission_id) + ON CONFLICT DO NOTHING.

DO $$
DECLARE
    c RECORD;
    inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id
        FROM roles r
        CROSS JOIN permissions p
        WHERE p.name IN ('ai:chat', 'ai:suggest')
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V052: concedeu ai:chat/ai:suggest a % vinculo(s) na empresa %', inserted, c.id;
        END IF;
    END LOOP;

    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;