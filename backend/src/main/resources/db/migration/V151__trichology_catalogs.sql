-- V151__trichology_catalogs.sql
-- Módulo Tricologia: configurações por empresa (protocolos de captura, catálogo
-- de achados, marcadores laboratoriais e limites). Seeds feitos no código
-- (TrichologySeeder), na primeira vez que a empresa TRICHOLOGY usa o módulo.

CREATE TABLE trichology_settings (
    company_id             UUID PRIMARY KEY REFERENCES companies (id),
    min_confidence         NUMERIC(3, 2) NOT NULL DEFAULT 0.60,
    report_horizons_months VARCHAR(40) NOT NULL DEFAULT '3,6,9,12',
    exam_rules             JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_trichology_settings_confidence CHECK (min_confidence >= 0 AND min_confidence <= 1)
);

CREATE TABLE trichology_capture_protocols (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies (id),
    name        VARCHAR(120) NOT NULL,
    regions     JSONB NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_trichology_protocols_company_id UNIQUE (company_id, id),
    CONSTRAINT ck_trichology_protocols_name CHECK (length(trim(name)) > 0)
);
CREATE UNIQUE INDEX uq_trichology_protocols_default
    ON trichology_capture_protocols (company_id) WHERE is_default AND active;

CREATE TABLE trichology_finding_catalog (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies (id),
    code        VARCHAR(40) NOT NULL,
    label       VARCHAR(120) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_trichology_findings_code UNIQUE (company_id, code),
    CONSTRAINT ck_trichology_findings_code CHECK (code ~ '^[A-Z0-9_]+$')
);

CREATE TABLE trichology_markers (
    id               UUID PRIMARY KEY,
    company_id       UUID NOT NULL REFERENCES companies (id),
    code             VARCHAR(40) NOT NULL,
    label            VARCHAR(120) NOT NULL,
    unit             VARCHAR(20),
    system           VARCHAR(40) NOT NULL,
    sex              VARCHAR(1),
    reference_min    NUMERIC(12, 4),
    reference_max    NUMERIC(12, 4),
    target_quartile  INTEGER,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_trichology_markers_code CHECK (code ~ '^[A-Z0-9_]+$'),
    CONSTRAINT ck_trichology_markers_sex CHECK (sex IS NULL OR sex IN ('F', 'M')),
    CONSTRAINT ck_trichology_markers_quartile CHECK (target_quartile IS NULL OR target_quartile BETWEEN 1 AND 4),
    CONSTRAINT ck_trichology_markers_range CHECK (reference_min IS NULL OR reference_max IS NULL
                                                  OR reference_min < reference_max)
);
CREATE UNIQUE INDEX uq_trichology_markers_code
    ON trichology_markers (company_id, code, COALESCE(sex, '-'));

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['trichology_settings', 'trichology_capture_protocols',
                             'trichology_finding_catalog', 'trichology_markers'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I
                            USING (company_id = app.current_tenant_id())
                            WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE ON %I TO crm_app', t);
    END LOOP;
END $$;
