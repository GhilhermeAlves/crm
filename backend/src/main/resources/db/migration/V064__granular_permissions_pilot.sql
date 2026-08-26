-- V064__granular_permissions_pilot.sql
-- Sprint 20 (Fase 1): autorização granular — piloto no módulo de Contatos.
--
-- Semântica definida (docs/SECURITY_MODEL.md):
--   * Permissões são aditivas (ALLOW); união das permissões dos perfis do usuário.
--   * Não há DENY nem override por usuário nesta fase (FUTURE).
--   * Novas granularidades:
--       <recurso>:page:view          acesso à página (menu/rota)
--       <recurso>:field:<campo>:update  edição de campo específico
--   * security:page:view controla o novo menu Segurança.
--
-- Padrão da V045/V060 (seed) + V053/V061 (grants). Idempotente.

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('contact:page:view',             'View Contacts page',                    'contacts', 'contact', 'page:view'),
    ('contact:field:email:update',    'Update contact email field',            'contacts', 'contact', 'field:update'),
    ('contact:field:phone:update',    'Update contact phone field',            'contacts', 'contact', 'field:update'),
    ('security:page:view',            'View Security menu (users/profiles)',   'settings', 'security', 'page:view')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name IN ('contact:page:view', 'contact:field:email:update',
                         'contact:field:phone:update', 'security:page:view')
          AND (
              (r.name IN ('ADMIN', 'MANAGER', 'AGENT')
                  AND p.name IN ('contact:page:view',
                                 'contact:field:email:update',
                                 'contact:field:phone:update'))
              OR (r.name = 'VIEWER' AND p.name = 'contact:page:view')
              OR (r.name = 'ADMIN' AND p.name = 'security:page:view')
          )
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V064: granted % granular permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
