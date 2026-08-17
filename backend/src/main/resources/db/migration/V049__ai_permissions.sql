-- V049__ai_permissions.sql
-- Sprint 20 (Módulo de IA): permissão de sugestão de resposta com IA.
-- Mesmo padrão da V045/V048: INSERT ... ON CONFLICT (name) DO NOTHING.
-- O vínculo papel -> permissão é aplicado no startup pelo RoleSeedService.
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('ai:suggest', 'Generate AI response suggestions', 'ai', 'suggestion', 'suggest')
ON CONFLICT (name) DO NOTHING;