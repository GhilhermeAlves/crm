-- V132__platform_super_admin.sql
-- Inverte os papéis de topo (decisão do dono do CRM em 11/11/2026):
--
--   SUPER_ADMIN  dono do CRM — único papel que enxerga e administra todas as
--                empresas. Existe só na empresa da plataforma, atribuído só por
--                migration (a API recusa criar/atribuir esse nome).
--   CRM_ADMIN    administrador de cada empresa cliente — gerencia só a própria
--                conta (era o SUPER_ADMIN semeado em toda empresa até a V131).
--
-- Até a V131 era o contrário (V112 criou CRM_ADMIN como papel de plataforma).
-- Os ids dos papéis são preservados: só o nome muda, então user_roles e
-- role_permissions continuam apontando para os mesmos registros.
--
-- Também:
--   * app.is_platform_admin_user(uuid) / app.is_platform_admin(): identificam o
--     dono do CRM a partir de qualquer empresa ativa (SECURITY DEFINER, pois
--     user_roles/roles têm RLS FORCE por empresa).
--   * app.is_super_admin() passa a verificar o USUÁRIO da sessão. A versão
--     anterior respondia "alguém da empresa ativa tem SUPER_ADMIN?", sem olhar
--     quem estava logado. Nenhuma policy nem código a usava.
--   * RLS em role_permissions: cada empresa só lê/grava os vínculos dos próprios
--     papéis (antes, sem RLS, o crm_app podia alterar papéis de outras empresas).
--
-- Roda como crm_admin (SPRING_FLYWAY_USER, superuser): o UPDATE dos dados não
-- passa pelo RLS. lock_timeout evita que o ALTER TABLE fique preso atrás de
-- transações longas da aplicação — se estourar, o Flyway falha sem aplicar nada
-- e o deploy pode ser repetido.

SET lock_timeout = '5s';

-- ---------------------------------------------------------------------------
-- 1. Troca dos nomes dos papéis (índice único em (name, company_id): passa por
--    um nome temporário).
-- ---------------------------------------------------------------------------
UPDATE roles SET name = '__V132_COMPANY_ADMIN' WHERE name = 'SUPER_ADMIN';

UPDATE roles
SET name = 'SUPER_ADMIN',
    description = 'Dono do CRM: acesso a todas as empresas',
    is_system = TRUE,
    updated_at = NOW()
WHERE name = 'CRM_ADMIN';

UPDATE roles
SET name = 'CRM_ADMIN',
    description = 'Administrador da empresa: gerencia a própria conta',
    updated_at = NOW()
WHERE name = '__V132_COMPANY_ADMIN';

-- Papel renomeado pela V112 para liberar o nome reservado.
UPDATE roles SET name = 'CRM_ADMIN_CUSTOM_OLD', updated_at = NOW() WHERE name = 'CRM_ADMIN_CUSTOM';

-- O papel de membership acompanha o nome do papel de topo da empresa.
UPDATE memberships SET role = 'CRM_ADMIN', updated_at = NOW() WHERE role = 'SUPER_ADMIN';
UPDATE invitations SET role = 'CRM_ADMIN', updated_at = NOW() WHERE role = 'SUPER_ADMIN';

-- ---------------------------------------------------------------------------
-- 2. Donos de empresa (membership OWNER ativa) passam a ter CRM_ADMIN no lugar
--    de ADMIN, como as empresas criadas daqui em diante.
-- ---------------------------------------------------------------------------
INSERT INTO user_roles (user_id, role_id, company_id)
SELECT m.user_id, r.id, m.company_id
FROM memberships m
JOIN roles r ON r.company_id = m.company_id AND r.name = 'CRM_ADMIN'
WHERE m.role = 'OWNER' AND m.status = 'ACTIVE'
ON CONFLICT (user_id, role_id) DO NOTHING;

DELETE FROM user_roles ur
USING memberships m, roles r
WHERE m.role = 'OWNER' AND m.status = 'ACTIVE'
  AND ur.user_id = m.user_id AND ur.company_id = m.company_id
  AND r.id = ur.role_id AND r.name = 'ADMIN';

-- ---------------------------------------------------------------------------
-- 3. Funções de identificação do dono do CRM.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app.is_platform_admin_user(p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id AND r.company_id = ur.company_id
        WHERE ur.user_id = p_user_id
          AND r.name = 'SUPER_ADMIN'
    );
$$;

-- Usuário da sessão: o TenantFilter define app.current_keycloak_sub em toda
-- requisição autenticada (e o e-mail para provedores externos sem sub vinculado).
CREATE OR REPLACE FUNCTION app.is_platform_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM users u
        WHERE app.is_platform_admin_user(u.id)
          AND (
              u.keycloak_sub = NULLIF(current_setting('app.current_keycloak_sub', TRUE), '')
              OR (u.keycloak_sub IS NULL
                  AND lower(u.email) = lower(NULLIF(current_setting('app.current_identity_email', TRUE), '')))
          )
    );
$$;

CREATE OR REPLACE FUNCTION app.is_super_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
    SELECT app.is_platform_admin();
$$;

REVOKE ALL ON FUNCTION app.is_platform_admin_user(UUID) FROM PUBLIC;
REVOKE ALL ON FUNCTION app.is_platform_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.is_platform_admin_user(UUID) TO crm_app;
GRANT EXECUTE ON FUNCTION app.is_platform_admin() TO crm_app;
GRANT EXECUTE ON FUNCTION app.is_super_admin() TO crm_app;

-- ---------------------------------------------------------------------------
-- 4. RLS em role_permissions: vínculos visíveis/graváveis só para os papéis da
--    empresa ativa. A subconsulta em roles já é filtrada pelo RLS de roles.
-- ---------------------------------------------------------------------------
ALTER TABLE role_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_permissions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS role_permissions_tenant_policy ON role_permissions;
CREATE POLICY role_permissions_tenant_policy ON role_permissions
    FOR ALL
    USING (role_id IN (SELECT r.id FROM roles r WHERE r.company_id = app.current_tenant_id()))
    WITH CHECK (role_id IN (SELECT r.id FROM roles r WHERE r.company_id = app.current_tenant_id()));

RESET lock_timeout;
