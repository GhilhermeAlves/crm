-- V042__workflow_permissions.sql
-- Sprint 14 (Workflow e Automação Comercial): permissões de gestão de workflows.
-- Segue o mesmo padrão da V038/V040 (INSERT ... ON CONFLICT (name) DO NOTHING).
-- A execução automática das ações (executor) é um comportamento de sistema e NÃO
-- depende de permissão de usuário (não exposta ao RBAC). O vínculo papel ->
-- permissão é aplicado no startup pelo RoleSeedService.
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('workflow:create', 'Create workflows', 'crm', 'workflow', 'create'),
    ('workflow:read', 'Read workflows', 'crm', 'workflow', 'read'),
    ('workflow:update', 'Update workflows', 'crm', 'workflow', 'update'),
    ('workflow:delete', 'Delete workflows', 'crm', 'workflow', 'delete')
ON CONFLICT (name) DO NOTHING;