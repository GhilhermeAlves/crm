-- V045__omnichannel_permissions.sql
-- Sprint 16: permissões do módulo omnichannel (canal + inbox/whatsapp).
-- Mesmo padrão da V042/V040: INSERT ... ON CONFLICT (name) DO NOTHING.
-- O vínculo papel -> permissão é aplicado no startup pelo RoleSeedService.
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('omnichannel:read',    'Read omnichannel channels and conversations', 'omnichannel', 'omnichannel', 'read'),
    ('omnichannel:create',  'Create omnichannel channels',                  'omnichannel', 'omnichannel', 'create'),
    ('omnichannel:update',  'Update omnichannel channels',                  'omnichannel', 'omnichannel', 'update'),
    ('omnichannel:delete',  'Delete omnichannel channels',                  'omnichannel', 'omnichannel', 'delete'),
    ('omnichannel:send',    'Send omnichannel messages',                    'omnichannel', 'omnichannel', 'send')
ON CONFLICT (name) DO NOTHING;
