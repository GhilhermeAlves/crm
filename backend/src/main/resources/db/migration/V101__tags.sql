-- Fase C da Agenda: Marcadores (tags) por empresa e vínculo com agendamentos/retornos.
-- A tabela "tags" já existe desde a V015 (etiquetas de contatos); por isso os marcadores
-- da agenda ficam em "schedule_tags".
-- Mantém a coluna recalls.tag antiga até fase futura (não removê-la).

CREATE TABLE schedule_tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    color      VARCHAR(7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_schedule_tags_company_name UNIQUE (company_id, name)
);

CREATE INDEX idx_schedule_tags_company ON schedule_tags (company_id);

CREATE TABLE appointment_tags (
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    tag_id          UUID NOT NULL REFERENCES schedule_tags(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (appointment_id, tag_id)
);

CREATE INDEX idx_appointment_tags_tag ON appointment_tags (company_id, tag_id);
CREATE INDEX idx_appointment_tags_appointment ON appointment_tags (appointment_id);

ALTER TABLE recalls ADD COLUMN tag_id UUID REFERENCES schedule_tags(id) ON DELETE SET NULL;
CREATE INDEX idx_recalls_company_tag_id ON recalls (company_id, tag_id);

-- Migração de dados: cria marcadores a partir dos textos existentes e preenche tag_id.
-- Mantém recalls.tag intacta.
INSERT INTO schedule_tags (id, company_id, name, color, created_at, updated_at)
SELECT DISTINCT ON (r.company_id, LOWER(TRIM(r.tag)))
       gen_random_uuid(),
       r.company_id,
       TRIM(r.tag),
       NULL,
       now(),
       now()
FROM recalls r
WHERE r.tag IS NOT NULL
  AND TRIM(r.tag) <> ''
ON CONFLICT DO NOTHING;

UPDATE recalls r
SET tag_id = t.id
FROM schedule_tags t
WHERE r.tag_id IS NULL
  AND r.tag IS NOT NULL
  AND TRIM(r.tag) <> ''
  AND t.company_id = r.company_id
  AND LOWER(t.name) = LOWER(TRIM(r.tag));

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['schedule_tags', 'appointment_tags']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

-- Papel da aplicação (padrão V087/V091+): sem isso, produção falha com "permission denied".
GRANT SELECT, INSERT, UPDATE, DELETE ON schedule_tags TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON appointment_tags TO crm_app;
