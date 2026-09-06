-- V074__followup_permissions.sql
-- Sprint 22 (FollowUp): permissões do módulo followup (ligado ao omnichannel).
--
--   omnichannel:followup:read   -> listar/consultar follow-ups
--   omnichannel:followup        -> agendar e cancelar follow-ups (manage)
--
-- Least privilege por papel:
--   ADMIN  : read + manage
--   MANAGER: read + manage
--   AGENT  : read + manage   (atendente agenda follow-up da conversa)
--   VIEWER : read somente
--   SUPER_ADMIN: via wildcard "*" do RoleSeedService (V069 garante empresas antigas).
--
-- Mesmo padrão V072: INSERT ... ON CONFLICT (name) DO NOTHING + DO block com
-- app.current_company_id por empresa (RLS exige tenant no INSERT de
-- role_permissions). Idempotente.

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('omnichannel:followup', 'Schedule and cancel conversation follow-ups', 'omnichannel', 'followup', 'manage'),
    ('omnichannel:followup:read', 'Read conversation follow-ups', 'omnichannel', 'followup', 'read')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name IN ('omnichannel:followup', 'omnichannel:followup:read')
          AND (
              (r.name IN ('ADMIN', 'MANAGER', 'AGENT'))
              OR (r.name = 'VIEWER' AND p.name = 'omnichannel:followup:read')
          )
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V074: granted % followup permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;