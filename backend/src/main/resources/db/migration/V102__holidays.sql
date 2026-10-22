-- Fase C da Agenda: Feriados da clínica (bloqueiam agendamento quando blocks_schedule = true).
-- Utiliza o padrão RLS igual a V098__recalls.sql.

CREATE TABLE company_holidays (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    date            DATE NOT NULL,
    name            VARCHAR(120) NOT NULL,
    recurring_yearly BOOLEAN NOT NULL DEFAULT FALSE,
    blocks_schedule  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_company_holidays_company_date UNIQUE (company_id, date)
);

CREATE INDEX idx_company_holidays_company_date ON company_holidays (company_id, date);

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['company_holidays']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

-- Papel da aplicação (padrão V087/V091+): sem isso, produção falha com "permission denied".
GRANT SELECT, INSERT, UPDATE, DELETE ON company_holidays TO crm_app;
