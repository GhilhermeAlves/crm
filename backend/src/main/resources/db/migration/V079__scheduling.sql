-- V079__scheduling.sql
-- Agenda de agendamentos (fase 1). Padrão V039: company_id NOT NULL, RLS FORCE,
-- tenant_isolation_policy. Grants via loop da V034.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointment_types (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id             UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                   VARCHAR(120) NOT NULL,
    slug                   VARCHAR(80)  NOT NULL,
    description            TEXT,
    duration_minutes       INT NOT NULL,
    buffer_before_minutes  INT NOT NULL DEFAULT 0,
    buffer_after_minutes   INT NOT NULL DEFAULT 0,
    min_notice_hours       INT NOT NULL DEFAULT 2,
    max_days_ahead         INT NOT NULL DEFAULT 60,
    slot_interval_minutes  INT NOT NULL DEFAULT 30,
    color                  VARCHAR(7),
    location_kind          VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_MEET',
    location_detail        VARCHAR(255),
    assignment_mode        VARCHAR(20) NOT NULL DEFAULT 'ROUND_ROBIN',
    public_booking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_appointment_types_slug UNIQUE (company_id, slug),
    CONSTRAINT chk_at_duration CHECK (duration_minutes BETWEEN 5 AND 480),
    CONSTRAINT chk_at_buffers CHECK (buffer_before_minutes BETWEEN 0 AND 240 AND buffer_after_minutes BETWEEN 0 AND 240),
    CONSTRAINT chk_at_interval CHECK (slot_interval_minutes IN (5, 10, 15, 20, 30, 60)),
    CONSTRAINT chk_at_location CHECK (location_kind IN ('GOOGLE_MEET', 'PHONE', 'IN_PERSON')),
    CONSTRAINT chk_at_assignment CHECK (assignment_mode IN ('ROUND_ROBIN', 'CHOOSE_HOST'))
);

CREATE TABLE appointment_type_hosts (
    appointment_type_id UUID NOT NULL REFERENCES appointment_types(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (appointment_type_id, user_id)
);

CREATE TABLE user_scheduling_settings (
    user_id     UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    timezone    VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE availability_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weekday     SMALLINT NOT NULL,           -- 1 = segunda ... 7 = domingo (ISO)
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    CONSTRAINT chk_ar_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT chk_ar_range CHECK (end_time > start_time)
);
CREATE INDEX idx_availability_rules_user ON availability_rules (company_id, user_id);

CREATE TABLE availability_overrides (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date        DATE NOT NULL,
    start_time  TIME,                        -- NULL/NULL = dia indisponível
    end_time    TIME,
    CONSTRAINT chk_ao_range CHECK ((start_time IS NULL AND end_time IS NULL) OR end_time > start_time)
);
CREATE INDEX idx_availability_overrides_user ON availability_overrides (company_id, user_id, date);

CREATE TABLE appointments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    appointment_type_id UUID REFERENCES appointment_types(id) ON DELETE SET NULL,
    host_id             UUID NOT NULL REFERENCES users(id),
    contact_id          UUID REFERENCES contacts(id) ON DELETE SET NULL,
    opportunity_id      UUID REFERENCES opportunities(id) ON DELETE SET NULL,
    title               VARCHAR(200) NOT NULL,
    start_at            TIMESTAMPTZ NOT NULL,
    end_at              TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    source              VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    location_kind       VARCHAR(20),
    location_detail     VARCHAR(255),
    meeting_url         VARCHAR(500),
    notes               TEXT,
    cancel_reason       VARCHAR(500),
    public_token        VARCHAR(64) NOT NULL UNIQUE,
    google_event_id     VARCHAR(255),
    created_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_appt_range CHECK (end_at > start_at),
    CONSTRAINT chk_appt_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT chk_appt_source CHECK (source IN ('INTERNAL', 'PUBLIC_LINK')),
    CONSTRAINT ex_appointments_host_no_overlap EXCLUDE USING gist (
        host_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status <> 'CANCELED')
);
CREATE INDEX idx_appointments_host_start ON appointments (company_id, host_id, start_at);
CREATE INDEX idx_appointments_contact ON appointments (contact_id);
CREATE INDEX idx_appointments_opportunity ON appointments (opportunity_id);

CREATE TABLE schedule_blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    host_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    reason      VARCHAR(200),
    source      VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    external_id VARCHAR(255),
    created_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_block_range CHECK (end_at > start_at),
    CONSTRAINT chk_block_source CHECK (source IN ('INTERNAL', 'GOOGLE'))
);
CREATE INDEX idx_schedule_blocks_host_start ON schedule_blocks (company_id, host_id, start_at);

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['appointment_types', 'appointment_type_hosts', 'user_scheduling_settings',
                             'availability_rules', 'availability_overrides', 'appointments', 'schedule_blocks']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('appointment:create',    'Create appointments',         'crm',    'appointment', 'create'),
    ('appointment:read',      'Read appointments',           'crm',    'appointment', 'read'),
    ('appointment:update',    'Update appointments',         'crm',    'appointment', 'update'),
    ('appointment:delete',    'Delete appointments',         'crm',    'appointment', 'delete'),
    ('scheduling:configure',  'Configure appointment types', 'crm',    'scheduling',  'configure'),
    ('appointment:page:view', 'View Agenda page',            'agenda', 'appointment', 'page:view')
ON CONFLICT (name) DO NOTHING;

-- Vincula às empresas existentes (padrão V066); novas empresas recebem via RoleSeedService.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (r.name IN ('ADMIN', 'MANAGER') AND p.name IN ('appointment:create', 'appointment:read',
                   'appointment:update', 'appointment:delete', 'scheduling:configure', 'appointment:page:view'))
           OR (r.name = 'AGENT' AND p.name IN ('appointment:create', 'appointment:read',
                   'appointment:update', 'appointment:page:view'))
           OR (r.name = 'VIEWER' AND p.name IN ('appointment:read', 'appointment:page:view'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
