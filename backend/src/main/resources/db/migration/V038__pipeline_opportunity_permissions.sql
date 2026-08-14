-- V038__pipeline_opportunity_permissions.sql
-- Sprint 11 (Pipeline): adiciona permissões granuladas de oportunidades.
--
-- As permissões de pipeline (pipeline:view / pipeline:update) já foram criadas
-- na V007 e referenciadas pelo RoleSeedService e pela sidebar do frontend.
-- Aqui adicionamos apenas o conjunto novo de oportunidades, no mesmo padrão da
-- V007 (INSERT ... ON CONFLICT (name) DO NOTHING).
--
-- O vínculo papel -> permissão é feito no startup pelo RoleSeedService
-- (role_permissions), que é reexecutado a cada deploy para todos os tenants.
INSERT INTO permissions (name, description, module, resource, action) VALUES
    -- Opportunities
    ('opportunity:create', 'Create opportunities', 'pipeline', 'opportunity', 'create'),
    ('opportunity:read', 'Read opportunities', 'pipeline', 'opportunity', 'read'),
    ('opportunity:update', 'Update opportunities', 'pipeline', 'opportunity', 'update'),
    ('opportunity:delete', 'Delete opportunities', 'pipeline', 'opportunity', 'delete'),
    ('opportunity:move', 'Move opportunities between stages', 'pipeline', 'opportunity', 'move'),
    ('opportunity:win', 'Mark opportunities as won', 'pipeline', 'opportunity', 'win'),
    ('opportunity:lose', 'Mark opportunities as lost', 'pipeline', 'opportunity', 'lose')
ON CONFLICT (name) DO NOTHING;
