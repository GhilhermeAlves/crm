-- V069__ensure_super_admin_permissions.sql
-- Fase 3.1 hotfix: ensure SUPER_ADMIN has ALL permissions in every company.
--
-- Root cause: V064/V066/V068 only granted new permissions to ADMIN (and sometimes
-- MANAGER/AGENT/VIEWER), never to SUPER_ADMIN. RoleDataSeeder only runs seedRoles()
-- for one company (the default). Result: SUPER_ADMIN in non-default companies was
-- missing ~28 permissions (security:page:view, *:page:view, role:manage, etc.).
--
-- This migration grants every permission in the catalog to every SUPER_ADMIN role,
-- across all existing companies. Idempotent (ON CONFLICT DO NOTHING).
-- Does NOT touch ADMIN/MANAGER/AGENT/VIEWER roles or user_permissions table.

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id
        FROM roles r
        CROSS JOIN permissions p
        WHERE r.name = 'SUPER_ADMIN'
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V069: granted % missing permissions to SUPER_ADMIN for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
