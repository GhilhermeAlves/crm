-- Fase C da Agenda: Cadeiras (consultórios) por empresa.
-- O agendamento aponta para uma cadeira opcional (ON DELETE SET NULL: excluir a
-- cadeira não cancela nem apaga os agendamentos que a usavam).

CREATE TABLE chairs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    color      VARCHAR(7),
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_chairs_company_name UNIQUE (company_id, name)
);

CREATE INDEX idx_chairs_company_active ON chairs (company_id, active, sort_order);

ALTER TABLE appointments ADD COLUMN chair_id UUID REFERENCES chairs(id) ON DELETE SET NULL;

CREATE INDEX idx_appointments_chair_start ON appointments (company_id, chair_id, start_at);

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['chairs']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

-- Papel da aplicação (padrão V087/V091+): sem isso, produção falha com "permission denied".
GRANT SELECT, INSERT, UPDATE, DELETE ON chairs TO crm_app;
