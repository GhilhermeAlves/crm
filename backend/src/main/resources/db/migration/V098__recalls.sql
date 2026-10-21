-- Fase B da Agenda: Retornos (recalls)
-- Tabela de retornos programados + tabela de descarte de sugestões geradas pelo sistema.

CREATE TABLE recalls (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id     UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    phone          VARCHAR(20),
    host_id        UUID NOT NULL REFERENCES users(id),
    tag            VARCHAR(120),
    due_date       DATE NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_recalls_status CHECK (status IN ('PENDING', 'SCHEDULED', 'DISMISSED')),
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    created_by     UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recalls_company_due_date ON recalls (company_id, due_date);
CREATE INDEX idx_recalls_company_status ON recalls (company_id, status);
CREATE INDEX idx_recalls_company_host_id ON recalls (company_id, host_id);
CREATE INDEX idx_recalls_company_contact_id ON recalls (company_id, contact_id);

-- Descarte de sugestões de retorno: uma linha por (empresa, contato) torna a
-- sugestão permanentemente invisível para aquele paciente.
CREATE TABLE recall_suggestion_dismissals (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_recall_suggestion_dismissals_company_contact UNIQUE (company_id, contact_id)
);

-- Índice de apoio à query agregada de sugestões (última consulta concluída por contato).
CREATE INDEX idx_appointments_company_status_contact_start
    ON appointments (company_id, status, contact_id, start_at DESC);

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['recalls', 'recall_suggestion_dismissals']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

-- Papel da aplicação (padrão V087/V091+): sem isso, produção falha com "permission denied".
GRANT SELECT, INSERT, UPDATE, DELETE ON recalls TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON recall_suggestion_dismissals TO crm_app;
