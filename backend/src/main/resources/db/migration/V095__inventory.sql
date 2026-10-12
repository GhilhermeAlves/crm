-- V095__inventory.sql
-- Controle de estoque dos itens de PRODUTO do catálogo (V078):
--   - catalog_items ganha unit, min_stock e quantity (cache; fonte da verdade
--     é inventory_movements, atualizada na mesma transação).
--   - category de produtos vira lista fixa (serviços seguem com texto livre).
--   - inventory_movements registra ENTRADA/SAIDA/AJUSTE por item.
-- Permissões: inventory:read (todos) e inventory:manage (ADMIN/MANAGER).

-- ===========================================================================
-- catalog_items (alterações)
-- ===========================================================================
ALTER TABLE catalog_items
    ADD COLUMN unit        VARCHAR(20),
    ADD COLUMN min_stock   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN quantity    INTEGER NOT NULL DEFAULT 0;

-- Categorias antigas de produtos que não estão na lista fixa deixam de ser
-- classificadas (NULL) para o usuário reclassificar. Serviços não são tocados.
-- Precisa vir ANTES do CHECK (que valida as linhas existentes) e rodar por
-- empresa: catalog_items tem RLS FORCE, então sem app.current_company_id o
-- UPDATE não enxerga nenhuma linha.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);
        UPDATE catalog_items
           SET category = NULL
         WHERE item_type = 'PRODUCT' AND category IS NOT NULL
           AND category NOT IN ('EPI','INSTRUMENTO','MATERIAL_DE_CONSUMO','MEDICAMENTO','OUTROS');
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;

ALTER TABLE catalog_items
    ADD CONSTRAINT chk_catalog_items_unit CHECK (unit IS NULL OR unit IN
        ('CAIXA','CARTELA','MILILITROS','PACOTE','ROLO','UNIDADE')),
    ADD CONSTRAINT chk_catalog_items_min_stock CHECK (min_stock >= 0),
    ADD CONSTRAINT chk_catalog_items_quantity  CHECK (quantity  >= 0),
    ADD CONSTRAINT chk_catalog_items_category  CHECK (
        item_type <> 'PRODUCT' OR category IS NULL OR category IN
        ('EPI','INSTRUMENTO','MATERIAL_DE_CONSUMO','MEDICAMENTO','OUTROS')),
    -- Alvo da FK composta de inventory_movements (impede movimento de um
    -- tenant apontar para item de outro: a checagem de FK ignora RLS).
    ADD CONSTRAINT uq_catalog_items_id_company UNIQUE (id, company_id);

CREATE INDEX idx_catalog_items_company_type ON catalog_items (company_id, item_type);

-- ===========================================================================
-- inventory_movements
-- ===========================================================================
CREATE TABLE inventory_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL,
    movement_type   VARCHAR(20) NOT NULL,
    delta           INTEGER NOT NULL,
    note            TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FK composta: o item precisa ser da MESMA empresa do movimento.
    CONSTRAINT fk_inventory_movements_item FOREIGN KEY (catalog_item_id, company_id)
        REFERENCES catalog_items (id, company_id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_movements_type CHECK
        (movement_type IN ('ENTRADA','SAIDA','AJUSTE'))
);

CREATE INDEX idx_inventory_movements_item
    ON inventory_movements (company_id, catalog_item_id, created_at DESC);

ALTER TABLE inventory_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_movements FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON inventory_movements
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_movements TO crm_app;

-- ===========================================================================
-- Permissões do módulo.
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('inventory:read',   'Read product stock',   'inventory', 'inventory', 'read'),
    ('inventory:manage', 'Manage product stock', 'inventory', 'inventory', 'manage')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (p.name = 'inventory:read' AND r.name IN ('ADMIN', 'MANAGER', 'AGENT', 'VIEWER'))
           OR (p.name = 'inventory:manage' AND r.name IN ('ADMIN', 'MANAGER'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V095: granted % inventory permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
