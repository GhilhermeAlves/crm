-- ===========================================================================
-- V107 — Orçamentos do paciente (ficha do paciente, Fase 3b)
--
-- budgets: orçamento do paciente (DRAFT, APPROVED ou LOST) com desconto e,
--   quando aprovado, condição de pagamento (forma, parcelas, 1º vencimento).
-- budget_items: procedimentos do orçamento (patient_procedures) com o valor
--   congelado no momento da inclusão e a posição de exibição.
-- budget_transactions: lançamentos financeiros (parcelas) gerados na aprovação.
-- Permissões reaproveitadas: contact:read / contact:update (+ finance:create
-- para gerar lançamentos).
-- ===========================================================================

CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    title           VARCHAR(160) NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    discount_cents  BIGINT NOT NULL DEFAULT 0,
    notes           TEXT,
    payment_method  VARCHAR(20),
    installments    INT,
    first_due_date  DATE,
    approved_at     TIMESTAMP,
    approved_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    lost_at         TIMESTAMP,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_budgets_status CHECK (status IN ('DRAFT', 'APPROVED', 'LOST')),
    CONSTRAINT chk_budgets_discount CHECK (discount_cents >= 0),
    CONSTRAINT chk_budgets_installments CHECK (installments IS NULL OR installments BETWEEN 1 AND 24),
    CONSTRAINT chk_budgets_payment_method CHECK (payment_method IS NULL OR payment_method IN
        ('BOLETO', 'CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'CASH', 'TRANSFER', 'CHECK', 'OTHER')),
    CONSTRAINT chk_budgets_approved CHECK (status <> 'APPROVED' OR approved_at IS NOT NULL)
);

CREATE INDEX idx_budgets_company_contact ON budgets (company_id, contact_id);

CREATE TABLE budget_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    budget_id     UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    procedure_id  UUID NOT NULL REFERENCES patient_procedures(id) ON DELETE CASCADE,
    price_cents   BIGINT NOT NULL,
    position      INT NOT NULL,
    CONSTRAINT chk_budget_items_price CHECK (price_cents >= 0),
    CONSTRAINT uq_budget_items_budget_procedure UNIQUE (budget_id, procedure_id)
);

CREATE INDEX idx_budget_items_procedure ON budget_items (procedure_id);

CREATE TABLE budget_transactions (
    budget_id       UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    transaction_id  UUID NOT NULL REFERENCES financial_transactions(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (budget_id, transaction_id)
);

-- ===========================================================================
-- RLS FORCE + GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['budgets', 'budget_items', 'budget_transactions']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I '
                    || 'USING (company_id = app.current_tenant_id()) '
                    || 'WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO crm_app', t);
    END LOOP;
END $$;
