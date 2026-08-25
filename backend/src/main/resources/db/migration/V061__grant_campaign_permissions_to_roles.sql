-- V061__grant_campaign_permissions_to_roles.sql
-- Grants campaign:* e template:* para ALL roles de ALL companies, seguindo a
-- matriz da Sprint 17 (least privilege):
--   ADMIN   : campaign read/create/update/delete/execute/view_metrics + template CRUD
--   MANAGER : campaign read/update/execute/view_metrics + template read/create/update
--   AGENT   : campaign read/execute/view_metrics + template read
--   VIEWER  : campaign read + template read
-- RLS note: itera companies setando app.current_company_id por company.
-- Idempotente: UNIQUE(role_id, permission_id) + ON CONFLICT DO NOTHING.
DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (p.name LIKE 'campaign:%' OR p.name LIKE 'template:%')
          AND (
              (r.name = 'ADMIN')
              OR (r.name = 'MANAGER' AND p.name NOT IN (
                      'campaign:create', 'campaign:delete', 'template:delete'))
              OR (r.name = 'AGENT' AND p.name IN (
                      'campaign:read', 'campaign:execute', 'campaign:view_metrics', 'template:read'))
              OR (r.name = 'VIEWER' AND p.name IN ('campaign:read', 'template:read'))
          )
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V061: granted % campaign/template permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
