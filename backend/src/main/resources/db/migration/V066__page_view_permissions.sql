-- V066__page_view_permissions.sql
-- Sprint 20 (Fase 2): <recurso>:page:view para os módulos do menu principal.
-- Espelha, por perfil, a matriz já existente de leitura (RoleSeedService):
-- quem pode ler o recurso pode acessar a página.

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('dashboard:page:view',    'View Dashboard page',        'dashboard', 'dashboard',    'page:view'),
    ('lead:page:view',         'View Leads page',            'leads',     'lead',         'page:view'),
    ('pipeline:page:view',     'View Pipeline page',         'pipeline',  'pipeline',     'page:view'),
    ('task:page:view',         'View Tasks page',            'tasks',     'task',         'page:view'),
    ('activity:page:view',     'View Activities page',       'activities','activity',     'page:view'),
    ('workflow:page:view',     'View Automations page',      'workflow',  'workflow',     'page:view'),
    ('notification:page:view', 'View Notifications page',    'notifications','notification','page:view'),
    ('campaign:page:view',     'View Campaigns page',        'campaign',  'campaign',     'page:view'),
    ('omnichannel:page:view',  'View Inbox/Channels page',   'omnichannel','omnichannel', 'page:view'),
    ('audit:page:view',        'View Audit page',            'settings',  'audit',        'page:view')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name LIKE '%:page:view'
          AND p.name <> 'contact:page:view'
          AND (
              (r.name IN ('ADMIN'))
              OR (r.name = 'MANAGER' AND p.name IN (
                      'dashboard:page:view', 'lead:page:view', 'pipeline:page:view',
                      'task:page:view', 'activity:page:view', 'workflow:page:view',
                      'notification:page:view', 'campaign:page:view',
                      'omnichannel:page:view', 'audit:page:view'))
              OR (r.name = 'AGENT' AND p.name IN (
                      'dashboard:page:view', 'lead:page:view', 'pipeline:page:view',
                      'task:page:view', 'activity:page:view', 'workflow:page:view',
                      'notification:page:view', 'campaign:page:view',
                      'omnichannel:page:view'))
              OR (r.name = 'VIEWER' AND p.name IN (
                      'dashboard:page:view', 'lead:page:view', 'pipeline:page:view',
                      'task:page:view', 'activity:page:view', 'workflow:page:view',
                      'notification:page:view', 'campaign:page:view',
                      'omnichannel:page:view'))
          )
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V066: granted % page:view permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
