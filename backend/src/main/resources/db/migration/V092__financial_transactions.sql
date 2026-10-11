-- V092__financial_transactions.sql
-- Lançamentos financeiros por empresa (tela /finance/control).
--
-- Valores monetários são guardados em centavos (BIGINT). "Atrasado" NÃO é um
-- status gravado: é PENDING com due_date < hoje, calculado na leitura.
--
-- Cada lançamento carrega company_id de forma desnormalizada para que a policy
-- de RLS FORCE seja direta (company_id = app.current_tenant_id()), na mesma
-- linha das demais features (V091).
--
-- Permissões:
--   finance:read   -> ADMIN/MANAGER (listar/consultar lançamentos)
--   finance:manage -> ADMIN/MANAGER (criar/editar/pagar/cancelar/excluir)

-- ===========================================================================
-- financial_transactions
-- ===========================================================================
CREATE TABLE financial_transactions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type                 VARCHAR(10) NOT NULL,
    description          VARCHAR(255) NOT NULL,
    category             VARCHAR(40) NOT NULL,
    amount_cents         BIGINT NOT NULL,
    fee_cents            BIGINT NOT NULL DEFAULT 0,
    payment_method       VARCHAR(20) NOT NULL,
    due_date             DATE NOT NULL,
    paid_at              DATE,
    status               VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    contact_id           UUID REFERENCES contacts(id) ON DELETE SET NULL,
    professional_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    catalog_item_id      UUID REFERENCES catalog_items(id) ON DELETE SET NULL,
    installment_group_id UUID,
    installment_number   INT,
    installment_total    INT,
    notes                TEXT,
    created_by           UUID,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_financial_transactions_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_financial_transactions_category CHECK (category IN
        ('PROCEDURE', 'CONSULTATION', 'PRODUCT_SALE', 'OTHER_INCOME',
         'RENT', 'PAYROLL', 'SUPPLIES', 'LAB', 'TAXES', 'MARKETING',
         'UTILITIES', 'EQUIPMENT', 'OTHER_EXPENSE')),
    CONSTRAINT chk_financial_transactions_amount CHECK (amount_cents > 0),
    CONSTRAINT chk_financial_transactions_fee CHECK (fee_cents >= 0),
    CONSTRAINT chk_financial_transactions_payment_method CHECK (payment_method IN
        ('BOLETO', 'CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'CASH', 'TRANSFER', 'CHECK', 'OTHER')),
    CONSTRAINT chk_financial_transactions_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_financial_transactions_paid_at CHECK (paid_at IS NULL OR status = 'PAID')
);

CREATE INDEX idx_financial_transactions_company_due ON financial_transactions (company_id, due_date);
CREATE INDEX idx_financial_transactions_company_paid ON financial_transactions (company_id, paid_at);
CREATE INDEX idx_financial_transactions_company_type_status
    ON financial_transactions (company_id, type, status);

-- ===========================================================================
-- RLS FORCE (isolamento por tenant)
-- ===========================================================================
ALTER TABLE financial_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE financial_transactions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON financial_transactions
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- Grants (tabela é acessada pelo role de aplicação crm_app)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON financial_transactions TO crm_app;

-- ===========================================================================
-- Permissões do módulo
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('finance:read',   'Read financial transactions',   'finance', 'finance', 'read'),
    ('finance:manage', 'Manage financial transactions', 'finance', 'finance', 'manage')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (p.name = 'finance:read' AND r.name IN ('ADMIN', 'MANAGER'))
           OR (p.name = 'finance:manage' AND r.name IN ('ADMIN', 'MANAGER'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V092: granted % finance permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
