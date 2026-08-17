-- V048__notification_permissions.sql
-- Módulo de Notificações: adiciona permissões granulares de notificações no
-- mesmo padrão da V040/V038/V007 (INSERT ... ON CONFLICT (name) DO NOTHING).
-- O vínculo papel -> permissão é feito no startup pelo RoleSeedService
-- (role_permissions), reexecutado a cada deploy para todos os tenants.
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('notification:create', 'Create notifications', 'crm', 'notification', 'create'),
    ('notification:read', 'Read notifications', 'crm', 'notification', 'read'),
    ('notification:update', 'Update notifications', 'crm', 'notification', 'update'),
    ('notification:delete', 'Delete notifications', 'crm', 'notification', 'delete')
ON CONFLICT (name) DO NOTHING;