-- V063__analytics_permissions.sql
-- Sprint 19: permissão de leitura do módulo de Analytics (dashboard read-only).
-- Mesmo padrão da V045/V060: INSERT ... ON CONFLICT DO NOTHING.
-- Vínculo papel -> permissão aplicado abaixo (padrão V053/V061).

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('analytics:read', 'Read analytics dashboard and KPIs', 'reports', 'analytics', 'read')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name = 'analytics:read'
          AND r.name IN ('ADMIN', 'MANAGER', 'AGENT', 'VIEWER')
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V063: granted % analytics permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
