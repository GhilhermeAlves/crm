-- V078__catalog_items.sql
-- Catálogo de produtos e serviços por empresa. Consultado pelo agente de IA do
-- WhatsApp (tool searchCatalog: só itens ativos, campos públicos) e mantido
-- pela equipe na página /catalog.
--
-- Recurso de tenant com RLS FORCE (padrão V076). Permissões:
--   catalog:read   -> todos os papéis (listar/consultar)
--   catalog:manage -> ADMIN/MANAGER (criar/editar/ativar/desativar)

-- ===========================================================================
-- catalog_items
-- ===========================================================================
CREATE TABLE catalog_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    item_type    VARCHAR(20) NOT NULL,
    name         VARCHAR(160) NOT NULL,
    description  TEXT,
    category     VARCHAR(80),
    price        NUMERIC(12, 2),
    currency     VARCHAR(3) NOT NULL DEFAULT 'BRL',
    sku          VARCHAR(64),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_catalog_items_type CHECK (item_type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT chk_catalog_items_price CHECK (price IS NULL OR price >= 0)
);

CREATE INDEX idx_catalog_items_company ON catalog_items (company_id);
CREATE INDEX idx_catalog_items_company_active ON catalog_items (company_id, active);
-- SKU opcional, mas único por empresa quando informado.
CREATE UNIQUE INDEX uq_catalog_items_company_sku ON catalog_items (company_id, sku) WHERE sku IS NOT NULL;

ALTER TABLE catalog_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE catalog_items FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON catalog_items
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON catalog_items TO crm_app;

-- ===========================================================================
-- Permissões do módulo.
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('catalog:read', 'Read the product/service catalog', 'catalog', 'catalog', 'read'),
    ('catalog:manage', 'Manage the product/service catalog', 'catalog', 'catalog', 'manage')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (p.name = 'catalog:read' AND r.name IN ('ADMIN', 'MANAGER', 'AGENT', 'VIEWER'))
           OR (p.name = 'catalog:manage' AND r.name IN ('ADMIN', 'MANAGER'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V078: granted % catalog permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
