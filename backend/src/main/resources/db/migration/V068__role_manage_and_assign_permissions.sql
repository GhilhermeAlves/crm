-- V068__role_manage_and_assign_permissions.sql
-- Fase 3.1: seed role:manage (frontend gate) and role:assign (backend gate).
--
-- role:manage  → settings/roles/page.tsx can("role:manage") controls edit UI
-- role:assign  → @PreAuthorize in UserController & RoleController for role-to-user assignment
--
-- Both were missing from the permissions catalog, causing:
--   - role:manage: settings/roles edit UI always read-only (checkboxes non-functional)
--   - role:assign: 403 on POST/DELETE /users/{id}/roles and /roles/user/{userId}

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('role:manage', 'Manage roles and their permissions', 'identity', 'role', 'manage'),
    ('role:assign', 'Assign roles to users',             'identity', 'role', 'assign')
ON CONFLICT (name) DO NOTHING;

-- Grant to ADMIN role across all companies.
-- roles table has RLS FORCE (V019), so we must set tenant context per company.
DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id
        FROM roles r
        CROSS JOIN permissions p
        WHERE p.name IN ('role:manage', 'role:assign')
          AND r.name = 'ADMIN'
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V068: granted role:manage,role:assign to ADMIN for company %', c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;

-- SUPER_ADMIN: handled automatically by RoleSeedService.* wildcard on next startup.
