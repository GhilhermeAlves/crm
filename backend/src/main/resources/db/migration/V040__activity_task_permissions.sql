-- V040__activity_task_permissions.sql
-- Sprint 12 (Activities + Tasks): adiciona permissões granulares de activities
-- e tasks, no mesmo padrão da V038/V007 (INSERT ... ON CONFLICT (name) DO NOTHING).
-- O vínculo papel -> permissão é feito no startup pelo RoleSeedService
-- (role_permissions), reexecutado a cada deploy para todos os tenants.
INSERT INTO permissions (name, description, module, resource, action) VALUES
    -- Activities
    ('activity:create', 'Create activities', 'crm', 'activity', 'create'),
    ('activity:read', 'Read activities', 'crm', 'activity', 'read'),
    ('activity:update', 'Update activities', 'crm', 'activity', 'update'),
    ('activity:delete', 'Delete activities', 'crm', 'activity', 'delete'),
    -- Tasks
    ('task:create', 'Create tasks', 'crm', 'task', 'create'),
    ('task:read', 'Read tasks', 'crm', 'task', 'read'),
    ('task:update', 'Update tasks', 'crm', 'task', 'update'),
    ('task:delete', 'Delete tasks', 'crm', 'task', 'delete'),
    -- Dashboard orientado à ação
    ('dashboard:operational', 'View operational action dashboard', 'crm', 'dashboard', 'operational')
ON CONFLICT (name) DO NOTHING;