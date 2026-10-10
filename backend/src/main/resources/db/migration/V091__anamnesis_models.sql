-- V091__anamnesis_models.sql
-- Modelos de anamnese por empresa (telas /settings/documents/anamnese).
--
-- A estrutura é hierárquica: anamnesis_models -> anamnesis_sections ->
-- anamnesis_questions -> anamnesis_question_options. Todas as tabelas carregam
-- company_id de forma desnormalizada para que a policy de RLS FORCE seja direta
-- (company_id = app.current_tenant_id()), na mesma linha das demais features.
--
-- O modelo padrão "Anamnese Odontológica Padrão" NÃO é semeado aqui: cada empresa
-- recebe sua própria instância editável sob demanda (provisionamento idempotente
-- no AnamnesisService), marcada por is_default. O índice único parcial garante no
-- máximo um padrão por empresa, mesmo sob concorrência.
--
-- Permissões:
--   anamnesis:read   -> todos os papéis (listar/consultar modelos)
--   anamnesis:manage -> ADMIN/MANAGER (criar/editar/excluir/ativar/desativar)

-- ===========================================================================
-- anamnesis_models (raiz)
-- ===========================================================================
CREATE TABLE anamnesis_models (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    version     INT NOT NULL DEFAULT 1,
    created_by  UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_anamnesis_models_version CHECK (version >= 1)
);

CREATE INDEX idx_anamnesis_models_company ON anamnesis_models (company_id);
CREATE INDEX idx_anamnesis_models_company_active ON anamnesis_models (company_id, is_active);
-- No máximo um modelo padrão por empresa.
CREATE UNIQUE INDEX uq_anamnesis_models_company_default
    ON anamnesis_models (company_id) WHERE is_default;

-- ===========================================================================
-- anamnesis_sections
-- ===========================================================================
CREATE TABLE anamnesis_sections (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id      UUID NOT NULL REFERENCES anamnesis_models(id) ON DELETE CASCADE,
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title         VARCHAR(160) NOT NULL,
    is_professional BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anamnesis_sections_model ON anamnesis_sections (model_id, sort_order);
CREATE INDEX idx_anamnesis_sections_company ON anamnesis_sections (company_id);

-- ===========================================================================
-- anamnesis_questions
-- ===========================================================================
CREATE TABLE anamnesis_questions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id              UUID NOT NULL REFERENCES anamnesis_sections(id) ON DELETE CASCADE,
    model_id                UUID NOT NULL REFERENCES anamnesis_models(id) ON DELETE CASCADE,
    company_id              UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    text                    TEXT NOT NULL,
    question_type           VARCHAR(30) NOT NULL,
    is_required             BOOLEAN NOT NULL DEFAULT FALSE,
    highlight               BOOLEAN NOT NULL DEFAULT FALSE,
    allow_complement        BOOLEAN NOT NULL DEFAULT FALSE,
    complement_label        VARCHAR(255),
    complement_trigger      VARCHAR(20) NOT NULL DEFAULT 'YES',
    complement_option_value VARCHAR(255),
    sort_order              INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_anamnesis_questions_type CHECK (question_type IN
        ('SHORT_TEXT', 'LONG_TEXT', 'YES_NO', 'YES_NO_WITH_TEXT', 'NUMBER', 'SINGLE_SELECT', 'MULTI_SELECT')),
    CONSTRAINT chk_anamnesis_questions_trigger CHECK (complement_trigger IN
        ('YES', 'NO', 'ALWAYS', 'OPTION'))
);

CREATE INDEX idx_anamnesis_questions_section ON anamnesis_questions (section_id, sort_order);
CREATE INDEX idx_anamnesis_questions_model ON anamnesis_questions (model_id);
CREATE INDEX idx_anamnesis_questions_company ON anamnesis_questions (company_id);

-- ===========================================================================
-- anamnesis_question_options
-- ===========================================================================
CREATE TABLE anamnesis_question_options (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id  UUID NOT NULL REFERENCES anamnesis_questions(id) ON DELETE CASCADE,
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    label        VARCHAR(255) NOT NULL,
    value        VARCHAR(255) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anamnesis_options_question ON anamnesis_question_options (question_id, sort_order);
CREATE INDEX idx_anamnesis_options_company ON anamnesis_question_options (company_id);

-- ===========================================================================
-- RLS FORCE (isolamento por tenant)
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['anamnesis_models', 'anamnesis_sections',
                             'anamnesis_questions', 'anamnesis_question_options']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I '
                    || 'USING (company_id = app.current_tenant_id()) '
                    || 'WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

-- ===========================================================================
-- Grants (tabelas são acessadas pelo role de aplicação crm_app)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON anamnesis_models TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON anamnesis_sections TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON anamnesis_questions TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON anamnesis_question_options TO crm_app;

-- ===========================================================================
-- Permissões do módulo
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('anamnesis:read',   'Read anamnesis models',                 'anamnesis', 'anamnesis', 'read'),
    ('anamnesis:manage', 'Manage anamnesis models and templates', 'anamnesis', 'anamnesis', 'manage')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (p.name = 'anamnesis:read' AND r.name IN ('ADMIN', 'MANAGER', 'AGENT', 'VIEWER'))
           OR (p.name = 'anamnesis:manage' AND r.name IN ('ADMIN', 'MANAGER'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V091: granted % anamnesis permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
