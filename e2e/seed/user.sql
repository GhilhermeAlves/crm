-- e2e/seed/user.sql
-- Task 4.2 — Usuário E2E na API do CRM (banco compartilhado crm_main).
--
-- Executa SOMENTE depois do backend healthcheck (Flyway + seeders de roles
-- concluídos), como usuário superuser (`crm_admin` do postgres do compose)
-- — superuser ignora RLS FORCE. É dev/CI apenas.
--
-- Por que seed manual e não auto-provisioning (Keycloak → cerco):
--   * o auto-provisioning cria o usuário com crm_enabled=false (Sem CRM Access);
--   * a resolução de identidade nos dois serviços (backend local/validate e
--     auth-service /auth/me) exige, além do users.ACTIVE, membership ATIVA e
--     role (AGENT) da empresa — ausentes na criação automática.
--
-- O E2E REAL (ci.compose.ci.yml) alinha-se com o realm dev
-- (infrastructure/keycloak/import/crm-realm-dev.json): e-mail, id Keycloak e
-- empresa default (00000000-0000-0000-0000-000000000001, V018/V012).

INSERT INTO users (id, email, password_hash, name, first_name, last_name,
                   company_id, is_active, status, crm_enabled)
VALUES ('11111111-1111-1111-1111-111111111111',
        'e2e.admin@crm.local',
        'e2e-local-seed-only', -- hash inerte: login deste usuário é 100% via Keycloak
        'Admin E2E',
        'Admin',
        'E2E',
        '00000000-0000-0000-0000-000000000001',
        TRUE,
        'ACTIVE',
        TRUE)
ON CONFLICT (email) DO NOTHING;

-- Role AGENT da empresa default (criada por V002/V018/RoleDataSeeder).
INSERT INTO user_roles (user_id, role_id, company_id)
SELECT '11111111-1111-1111-1111-111111111111', r.id, r.company_id
FROM roles r
WHERE r.name = 'AGENT'
  AND r.company_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

-- Membership ATIVA (V030) — sem ela a resolução de identidade nega o acesso.
INSERT INTO memberships (user_id, company_id, role, status)
VALUES ('11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000001',
        'AGENT',
        'ACTIVE')
ON CONFLICT DO NOTHING;