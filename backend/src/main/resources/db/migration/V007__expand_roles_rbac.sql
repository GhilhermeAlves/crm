-- V007__expand_roles_rbac.sql
-- Expand roles table and seed comprehensive permissions

-- Add description, is_system, is_active to roles table
ALTER TABLE roles ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Expand V003 permissions seed with comprehensive module permissions
INSERT INTO permissions (name, description, module, resource, action) VALUES
    -- Dashboard
    ('dashboard:view', 'View dashboard', 'dashboard', 'dashboard', 'view'),
    -- Leads
    ('lead:create', 'Create leads', 'leads', 'lead', 'create'),
    ('lead:read', 'Read leads', 'leads', 'lead', 'read'),
    ('lead:update', 'Update leads', 'leads', 'lead', 'update'),
    ('lead:delete', 'Delete leads', 'leads', 'lead', 'delete'),
    -- Contacts
    ('contact:create', 'Create contacts', 'contacts', 'contact', 'create'),
    ('contact:read', 'Read contacts', 'contacts', 'contact', 'read'),
    ('contact:update', 'Update contacts', 'contacts', 'contact', 'update'),
    ('contact:delete', 'Delete contacts', 'contacts', 'contact', 'delete'),
    -- Pipeline
    ('pipeline:view', 'View pipeline', 'pipeline', 'pipeline', 'view'),
    ('pipeline:update', 'Update pipeline', 'pipeline', 'pipeline', 'update'),
    -- Chat
    ('chat:view', 'View chat', 'chat', 'chat', 'view'),
    ('chat:send', 'Send messages', 'chat', 'chat', 'send'),
    -- Campaigns
    ('campaign:create', 'Create campaigns', 'campaigns', 'campaign', 'create'),
    ('campaign:read', 'Read campaigns', 'campaigns', 'campaign', 'read'),
    ('campaign:update', 'Update campaigns', 'campaigns', 'campaign', 'update'),
    ('campaign:delete', 'Delete campaigns', 'campaigns', 'campaign', 'delete'),
    -- Reports
    ('report:view', 'View reports', 'reports', 'report', 'view'),
    ('report:export', 'Export reports', 'reports', 'report', 'export'),
    -- Settings
    ('settings:view', 'View settings', 'settings', 'settings', 'view'),
    ('settings:update', 'Update settings', 'settings', 'settings', 'update'),
    -- Company
    ('company:view', 'View companies', 'identity', 'company', 'view'),
    ('company:create', 'Create companies', 'identity', 'company', 'create'),
    ('company:update', 'Update companies', 'identity', 'company', 'update'),
    ('company:delete', 'Delete companies', 'identity', 'company', 'delete')
ON CONFLICT (name) DO NOTHING;
