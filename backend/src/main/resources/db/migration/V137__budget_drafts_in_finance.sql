-- ===========================================================================
-- V137: orçamento em rascunho aparece no financeiro (como no Capim).
-- A partir de agora, criar/editar um orçamento mantém um lançamento de receita
-- pendente ("Orçamento: <título>", valor total) ligado em budget_transactions;
-- aprovar troca esse lançamento pelas parcelas. Aqui lançamos os rascunhos que
-- já existiam. RLS FORCE: ajusta o tenant empresa a empresa (padrão da V112).
-- ===========================================================================
DO $$
DECLARE
    c RECORD;
    b RECORD;
    tx_id UUID;
BEGIN
    FOR c IN SELECT DISTINCT company_id FROM budgets WHERE status = 'DRAFT' LOOP
        PERFORM set_config('app.current_company_id', c.company_id::text, false);

        FOR b IN
            SELECT bu.id, bu.company_id, bu.contact_id, bu.title, bu.created_by, bu.created_at,
                   COALESCE((SELECT SUM(i.price_cents) FROM budget_items i WHERE i.budget_id = bu.id), 0)
                       - bu.discount_cents AS total_cents
            FROM budgets bu
            WHERE bu.company_id = c.company_id
              AND bu.status = 'DRAFT'
              AND NOT EXISTS (SELECT 1 FROM budget_transactions bt WHERE bt.budget_id = bu.id)
        LOOP
            CONTINUE WHEN b.total_cents <= 0;
            tx_id := gen_random_uuid();
            INSERT INTO financial_transactions
                (id, company_id, type, description, category, amount_cents, fee_cents, payment_method,
                 due_date, status, contact_id, created_by)
            VALUES
                (tx_id, b.company_id, 'INCOME', LEFT('Orçamento: ' || b.title, 255), 'PROCEDURE',
                 b.total_cents, 0, 'OTHER', b.created_at::date, 'PENDING', b.contact_id, b.created_by);
            INSERT INTO budget_transactions (budget_id, transaction_id, company_id)
            VALUES (b.id, tx_id, b.company_id);
        END LOOP;
    END LOOP;

    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
