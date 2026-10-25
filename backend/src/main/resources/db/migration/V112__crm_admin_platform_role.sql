-- V112__crm_admin_platform_role.sql
-- Papel de plataforma CRM_ADMIN, acima do SUPER_ADMIN.
--
-- Até aqui o SUPER_ADMIN (criado em toda empresa) enxergava e administrava
-- todas as empresas. A partir desta versão só o CRM_ADMIN cruza empresas; o
-- SUPER_ADMIN fica restrito à própria empresa.
--
-- O CRM_ADMIN NÃO é criado por empresa (RoleSeedService não o conhece) e a API
-- recusa criar/atribuir um papel com esse nome. Ele existe só na empresa do
-- usuário abaixo e é atribuído apenas por migration.
--
-- RLS: roles/users/user_roles têm RLS FORCE e o usuário de app é NOBYPASSRLS,
-- então iteramos as empresas definindo app.current_company_id (padrão V052/V069).
--
-- Idempotente. Se o e-mail não existir (ex.: banco do E2E), só registra NOTICE.

DO $$
DECLARE
    c RECORD;
    target_user UUID;
    platform_role UUID;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        -- Nome reservado: um papel personalizado CRM_ADMIN de outra empresa daria
        -- acesso a todas as empresas — renomeia antes de conceder o papel real.
        UPDATE roles SET name = 'CRM_ADMIN_CUSTOM'
        WHERE name = 'CRM_ADMIN' AND company_id = c.id
          AND NOT EXISTS (
              SELECT 1 FROM users u
              WHERE u.company_id = c.id AND lower(u.email) = 'ghilherme007@gmail.com'
          );
        IF FOUND THEN
            RAISE NOTICE 'V112: papel CRM_ADMIN personalizado renomeado na empresa %', c.id;
        END IF;

        SELECT u.id INTO target_user
        FROM users u
        WHERE u.company_id = c.id AND lower(u.email) = 'ghilherme007@gmail.com'
        LIMIT 1;

        IF target_user IS NOT NULL THEN
            INSERT INTO roles (name, company_id, description, is_system, is_active)
            VALUES ('CRM_ADMIN', c.id, 'Administrador do CRM: acesso a todas as empresas', TRUE, TRUE)
            ON CONFLICT (name, company_id) DO NOTHING;

            SELECT r.id INTO platform_role
            FROM roles r
            WHERE r.name = 'CRM_ADMIN' AND r.company_id = c.id;

            INSERT INTO role_permissions (role_id, permission_id)
            SELECT platform_role, p.id FROM permissions p
            ON CONFLICT (role_id, permission_id) DO NOTHING;

            INSERT INTO user_roles (user_id, role_id, company_id)
            VALUES (target_user, platform_role, c.id)
            ON CONFLICT (user_id, role_id) DO NOTHING;

            RAISE NOTICE 'V112: CRM_ADMIN concedido ao usuário % na empresa %', target_user, c.id;
        END IF;
    END LOOP;

    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
