-- Grants omnichannel:* permissions to ALL roles of ALL companies, following the
-- Sprint 16 authorization matrix (least privilege):
--   ADMIN   : read, create, update, delete, send
--   MANAGER : read, update, send
--   AGENT   : read, send
--   VIEWER  : read
-- Reason: RoleSeedService binds role->perm at startup but existing roles lacked
-- omnichannel:* (SUPER_ADMIN's '*' wildcard was a no-op), so only SUPER_ADMIN
-- could use the Inbox.
-- RLS note: iterates companies setting app.current_company_id per company.
-- Idempotent: UNIQUE(role_id, permission_id) + ON CONFLICT DO NOTHING.
DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name IN ('omnichannel:read', 'omnichannel:create', 'omnichannel:update',
                         'omnichannel:delete', 'omnichannel:send')
          AND (
              (r.name = 'ADMIN')
              OR (r.name = 'MANAGER' AND p.name IN ('omnichannel:read', 'omnichannel:update', 'omnichannel:send'))
              OR (r.name = 'AGENT' AND p.name IN ('omnichannel:read', 'omnichannel:send'))
              OR (r.name = 'VIEWER' AND p.name = 'omnichannel:read')
          )
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V053: granted % omnichannel permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
