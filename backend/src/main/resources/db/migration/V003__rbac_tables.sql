-- V003__rbac_tables.sql
-- RBAC: permissions and role_permissions tables

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL,
    module VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_permissions_name ON permissions(name);
CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module);

CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Seed default permissions
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('user:create', 'Create users', 'identity', 'user', 'create'),
    ('user:read', 'Read users', 'identity', 'user', 'read'),
    ('user:update', 'Update users', 'identity', 'user', 'update'),
    ('user:delete', 'Delete users', 'identity', 'user', 'delete'),
    ('user:invite', 'Invite users', 'identity', 'user', 'invite'),
    ('role:create', 'Create roles', 'identity', 'role', 'create'),
    ('role:read', 'Read roles', 'identity', 'role', 'read'),
    ('role:update', 'Update roles', 'identity', 'role', 'update'),
    ('role:delete', 'Delete roles', 'identity', 'role', 'delete'),
    ('permission:assign', 'Assign permissions', 'identity', 'permission', 'assign')
ON CONFLICT (name) DO NOTHING;
