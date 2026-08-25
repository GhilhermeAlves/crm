-- V060__campaign_permissions.sql
-- Sprint 17: permissões dos módulos campaign + template.
-- Mesmo padrão da V045/V048: INSERT ... ON CONFLICT (name) DO NOTHING.
-- Vínculo papel -> permissão aplicado na V061 (padrão V053) e pelo RoleSeedService.

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('campaign:read',         'Read campaigns',                    'campaign', 'campaign', 'read'),
    ('campaign:create',       'Create campaigns',                  'campaign', 'campaign', 'create'),
    ('campaign:update',       'Update campaigns',                  'campaign', 'campaign', 'update'),
    ('campaign:delete',       'Delete campaigns',                  'campaign', 'campaign', 'delete'),
    ('campaign:execute',      'Schedule, execute, pause and cancel campaigns', 'campaign', 'campaign', 'execute'),
    ('campaign:view_metrics', 'View campaign metrics and events',  'campaign', 'campaign', 'view_metrics'),
    ('template:read',         'Read message templates',            'template', 'template', 'read'),
    ('template:create',       'Create message templates',          'template', 'template', 'create'),
    ('template:update',       'Update message templates',          'template', 'template', 'update'),
    ('template:delete',       'Delete message templates',          'template', 'template', 'delete')
ON CONFLICT (name) DO NOTHING;
