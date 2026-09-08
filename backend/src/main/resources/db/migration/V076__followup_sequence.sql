-- V076__followup_sequence.sql
-- Sprint 22 (FollowUp Sequence): sequências reutilizáveis de follow-ups.
--
-- Uma sequência agrupa follow-ups programados (ex.: carrinho abandonado com
-- follow-up em 2h, 24h e 72h). O followup_sequences é um recurso de tenant
-- (por empresa) com RLS FORCE, mesmo padrão V073. Cada follow-up pode referenciar
-- opcionalmente a sequência que o originou (coluna follows.sequence_id nullable).
--
-- Permissões (least privilege): CRUD de sequências restrito a ADMIN/MANAGER:
--   omnichannel:followup:sequence:manage -> criar/editar/excluir/inativar
--   omnichannel:followup:sequence:read   -> listar/consultar

-- ===========================================================================
-- followup_sequences — grupo nomeado de follow-ups por empresa.
-- ===========================================================================
CREATE TABLE followup_sequences (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name         VARCHAR(120) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_followup_sequences_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uq_followup_sequences_company_name UNIQUE (company_id, name)
);

CREATE INDEX idx_followup_sequences_company ON followup_sequences (company_id);

ALTER TABLE followup_sequences ENABLE ROW LEVEL SECURITY;
ALTER TABLE followup_sequences FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON followup_sequences
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- followups.sequence_id — referência opcional à sequência de origem.
-- ===========================================================================
ALTER TABLE followups
    ADD COLUMN sequence_id UUID REFERENCES followup_sequences(id) ON DELETE SET NULL;

CREATE INDEX idx_followups_sequence ON followups (sequence_id);

-- ===========================================================================
-- Grants CRUD da tabela nova para crm_app (loop dinâmico, padrão V062);
-- GRANT não afeta RLS (crm_app é NOBYPASSRLS).
-- ===========================================================================
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT IN ('flyway_schema_history', 'permissions')
        ORDER BY tablename
    LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO crm_app', t);
    END LOOP;
END $$;

-- ===========================================================================
-- Permissões do módulo (CRUD de sequências restrito a ADMIN/MANAGER).
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('omnichannel:followup:sequence', 'Manage follow-up sequences', 'omnichannel', 'followup-sequence', 'manage'),
    ('omnichannel:followup:sequence:read', 'Read follow-up sequences', 'omnichannel', 'followup-sequence', 'read')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name IN ('omnichannel:followup:sequence', 'omnichannel:followup:sequence:read')
          AND r.name IN ('ADMIN', 'MANAGER')
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V076: granted % followup-sequence permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
