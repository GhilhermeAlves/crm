-- V065__user_permission_overrides.sql
-- Sprint 20 (Fase 2): overrides individuais de permissão por usuário.
--
-- Política (docs/SECURITY_MODEL.md):
--   INHERIT = ausência de linha → usuário segue as permissões dos perfis
--   ALLOW   = concede mesmo sem perfil conceder
--   DENY    = nega mesmo que um perfil conceda (precedência sobre ALLOW)
--
-- Efetiva = (união das permissões dos perfis ∪ ALLOW) − DENY
--
-- Padrão V044/V056: company_id NOT NULL FK, timestamps, índices,
-- RLS FORCE + policy tenant_isolation_policy. Idempotente.

CREATE TABLE user_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    effect        VARCHAR(8) NOT NULL CHECK (effect IN ('ALLOW', 'DENY')),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_user_permissions_user_permission
    ON user_permissions (user_id, permission_id);
CREATE INDEX idx_user_permissions_company ON user_permissions (company_id);

ALTER TABLE user_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_permissions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON user_permissions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
