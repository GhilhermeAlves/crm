-- V147__persons_core.sql
-- Cadastro central de pessoas (doc "Arquitetura do Cadastro de Pessoas" v1.4).
--
-- 1. persons + tabelas filhas (endereços, contatos, documentos, contas, relacionamentos,
--    histórico, pendências da migração).
-- 2. Papéis por composição, com a mesma chave da pessoa: customers -> patients, suppliers,
--    employees (+ cargos, histórico de cargos e especialidades).
-- 3. Validação de CPF/CNPJ (CNPJ alfanumérico) em SQL.
-- 4. Sincronização de transição: contacts continua sendo gravado pelas telas e integrações
--    atuais; um gatilho BEFORE INSERT/UPDATE mantém persons, o endereço, os contatos e o
--    cliente/paciente equivalentes (mesmo UUID). As telas novas gravam em persons e
--    devolvem os valores para contacts. Removido na etapa 8 (limpeza).
-- 5. Backfill por empresa (contacts tem RLS FORCE) na ordem de criação: o cadastro mais
--    antigo fica com o CPF; repetidos e inválidos vão para person_migration_issues.
-- 6. FK contacts(company_id, id) -> persons.
-- 7. Permissões e módulos.

-- ===========================================================================
-- 1. Validação de documento
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.is_valid_cpf(p TEXT)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s INT; r INT; i INT;
BEGIN
    IF p IS NULL OR p !~ '^[0-9]{11}$' OR p ~ '^(.)\1{10}$' THEN
        RETURN FALSE;
    END IF;
    s := 0;
    FOR i IN 1..9 LOOP s := s + substr(p, i, 1)::INT * (11 - i); END LOOP;
    r := (s * 10) % 11; IF r = 10 THEN r := 0; END IF;
    IF r <> substr(p, 10, 1)::INT THEN RETURN FALSE; END IF;
    s := 0;
    FOR i IN 1..10 LOOP s := s + substr(p, i, 1)::INT * (12 - i); END LOOP;
    r := (s * 10) % 11; IF r = 10 THEN r := 0; END IF;
    RETURN r = substr(p, 11, 1)::INT;
END $$;

-- CNPJ numérico ou alfanumérico (IN RFB 2.229/2024): valor do caractere = ASCII - 48.
CREATE OR REPLACE FUNCTION app.is_valid_cnpj(p TEXT)
RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE w1 INT[] := ARRAY[5,4,3,2,9,8,7,6,5,4,3,2];
        w2 INT[] := ARRAY[6,5,4,3,2,9,8,7,6,5,4,3,2];
        s INT; r INT; d1 INT; d2 INT; i INT;
BEGIN
    IF p IS NULL OR p !~ '^[0-9A-Z]{12}[0-9]{2}$' OR p ~ '^(.)\1{13}$' THEN
        RETURN FALSE;
    END IF;
    s := 0;
    FOR i IN 1..12 LOOP s := s + (ascii(substr(p, i, 1)) - 48) * w1[i]; END LOOP;
    r := s % 11; d1 := CASE WHEN r < 2 THEN 0 ELSE 11 - r END;
    IF d1 <> substr(p, 13, 1)::INT THEN RETURN FALSE; END IF;
    s := 0;
    FOR i IN 1..13 LOOP s := s + (ascii(substr(p, i, 1)) - 48) * w2[i]; END LOOP;
    r := s % 11; d2 := CASE WHEN r < 2 THEN 0 ELSE 11 - r END;
    RETURN d2 = substr(p, 14, 1)::INT;
END $$;

GRANT EXECUTE ON FUNCTION app.is_valid_cpf(TEXT) TO crm_app;
GRANT EXECUTE ON FUNCTION app.is_valid_cnpj(TEXT) TO crm_app;

-- ===========================================================================
-- 2. persons
-- ===========================================================================
CREATE TABLE persons (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    person_type              CHAR(1) NOT NULL DEFAULT 'F',
    name                     VARCHAR(150) NOT NULL,
    trade_name               VARCHAR(150),
    tax_id                   VARCHAR(14),
    rg_or_ie                 VARCHAR(30),
    rg_issuer                VARCHAR(20),
    birth_or_foundation_date DATE,
    gender                   VARCHAR(20),
    marital_status           VARCHAR(30),
    professional_status      VARCHAR(20),
    nationality              VARCHAR(60),
    country_id               INTEGER,
    provisional              BOOLEAN NOT NULL DEFAULT FALSE,
    active                   BOOLEAN NOT NULL DEFAULT TRUE,
    notes                    TEXT,
    avatar_url               TEXT,
    deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMPTZ,
    deleted_by               UUID REFERENCES users(id) ON DELETE SET NULL,
    delete_reason            TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by               UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by               UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_persons_type CHECK (person_type IN ('F', 'J')),
    CONSTRAINT ck_persons_tax_id CHECK (tax_id IS NULL
        OR (person_type = 'F' AND tax_id ~ '^[0-9]{11}$')
        OR (person_type = 'J' AND tax_id ~ '^[0-9A-Z]{12}[0-9]{2}$')),
    CONSTRAINT ck_persons_deleted CHECK (deleted = (deleted_at IS NOT NULL)),
    CONSTRAINT ck_persons_name CHECK (length(trim(name)) > 0),
    CONSTRAINT uq_persons_company_id UNIQUE (company_id, id)
);
CREATE UNIQUE INDEX uq_persons_tax_id ON persons (company_id, tax_id)
    WHERE tax_id IS NOT NULL AND deleted = FALSE;
CREATE INDEX idx_persons_name ON persons (company_id, lower(name));
CREATE INDEX idx_persons_active ON persons (company_id, active) WHERE deleted = FALSE;

-- ===========================================================================
-- 3. Tabelas filhas
-- ===========================================================================
CREATE TABLE person_addresses (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL,
    person_id         UUID NOT NULL,
    address_type      VARCHAR(20) NOT NULL DEFAULT 'RESIDENTIAL',
    postal_code       VARCHAR(8),
    street            VARCHAR(160),
    address_number    VARCHAR(20),
    complement        VARCHAR(80),
    neighborhood_id   UUID,
    neighborhood_name VARCHAR(80),
    city_id           INTEGER,
    city_name         VARCHAR(80),
    uf                CHAR(2),
    country_id        INTEGER,
    foreign_region    VARCHAR(80),
    is_primary        BOOLEAN NOT NULL DEFAULT FALSE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    sync_slot         VARCHAR(30),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_person_addresses_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_addresses_type CHECK (address_type IN
        ('RESIDENTIAL', 'COMMERCIAL', 'BILLING', 'DELIVERY', 'OTHER')),
    CONSTRAINT ck_person_addresses_postal_code CHECK (postal_code IS NULL OR postal_code ~ '^[0-9]{8}$')
);
CREATE INDEX idx_person_addresses_person ON person_addresses (company_id, person_id);
CREATE UNIQUE INDEX uq_person_addresses_primary ON person_addresses (person_id, address_type)
    WHERE is_primary AND active;
CREATE UNIQUE INDEX uq_person_addresses_slot ON person_addresses (person_id, sync_slot)
    WHERE sync_slot IS NOT NULL;

CREATE TABLE person_contacts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL,
    person_id     UUID NOT NULL,
    contact_type  VARCHAR(20) NOT NULL,
    value         VARCHAR(255) NOT NULL,
    contact_name  VARCHAR(100),
    is_whatsapp   BOOLEAN NOT NULL DEFAULT FALSE,
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    sync_slot     VARCHAR(30),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_person_contacts_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_contacts_type CHECK (contact_type IN ('MOBILE', 'PHONE', 'EMAIL', 'WEBSITE', 'OTHER')),
    CONSTRAINT ck_person_contacts_value CHECK (length(trim(value)) > 0)
);
CREATE INDEX idx_person_contacts_person ON person_contacts (company_id, person_id);
CREATE INDEX idx_person_contacts_value ON person_contacts (company_id, value);
CREATE UNIQUE INDEX uq_person_contacts_primary ON person_contacts (person_id, contact_type)
    WHERE is_primary AND active;
CREATE UNIQUE INDEX uq_person_contacts_slot ON person_contacts (person_id, sync_slot)
    WHERE sync_slot IS NOT NULL;

CREATE TABLE person_documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL,
    person_id         UUID NOT NULL,
    document_type     VARCHAR(20) NOT NULL,
    document_number   VARCHAR(30) NOT NULL,
    issuer            VARCHAR(20),
    issuer_uf         CHAR(2),
    issue_date        DATE,
    expiry_date       DATE,
    storage_object_id UUID REFERENCES storage_objects(id) ON DELETE SET NULL,
    is_primary        BOOLEAN NOT NULL DEFAULT FALSE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_person_documents_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_documents_type CHECK (document_type IN
        ('RG', 'CNH', 'PASSPORT', 'CNS', 'RNE', 'SOCIAL_CONTRACT', 'OTHER'))
);
CREATE INDEX idx_person_documents_person ON person_documents (company_id, person_id);

-- Campos sensíveis (agência, conta, dígito, chave PIX) chegam criptografados pela aplicação.
CREATE TABLE person_bank_accounts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL,
    person_id      UUID NOT NULL,
    bank_code      VARCHAR(3),
    branch_number  VARCHAR(255),
    account_number VARCHAR(255),
    account_digit  VARCHAR(255),
    account_type   VARCHAR(20),
    pix_key        VARCHAR(512),
    pix_key_type   VARCHAR(20),
    holder_name    VARCHAR(150),
    is_primary     BOOLEAN NOT NULL DEFAULT FALSE,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_person_bank_accounts_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_bank_accounts_type CHECK (account_type IS NULL OR account_type IN
        ('CHECKING', 'SAVINGS', 'PAYMENT', 'OTHER')),
    CONSTRAINT ck_person_bank_accounts_pix_type CHECK (pix_key_type IS NULL OR pix_key_type IN
        ('CPF', 'CNPJ', 'EMAIL', 'PHONE', 'RANDOM'))
);
CREATE INDEX idx_person_bank_accounts_person ON person_bank_accounts (company_id, person_id);

CREATE TABLE person_relationships (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL,
    person_id         UUID NOT NULL,
    related_person_id UUID NOT NULL,
    relationship_type VARCHAR(30) NOT NULL,
    start_date        DATE,
    end_date          DATE,
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_person_relationships_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_person_relationships_related FOREIGN KEY (company_id, related_person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_relationships_type CHECK (relationship_type IN
        ('GUARDIAN', 'SPOUSE', 'PARTNER', 'LEGAL_REPRESENTATIVE', 'OTHER')),
    CONSTRAINT ck_person_relationships_self CHECK (person_id <> related_person_id),
    CONSTRAINT uq_person_relationships UNIQUE (company_id, person_id, related_person_id, relationship_type)
);
CREATE INDEX idx_person_relationships_related ON person_relationships (company_id, related_person_id);

CREATE TABLE person_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL,
    person_id      UUID NOT NULL,
    user_id        UUID,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    operation      VARCHAR(20) NOT NULL,
    table_name     VARCHAR(60) NOT NULL,
    old_values     JSONB,
    new_values     JSONB,
    reason         TEXT,
    transaction_id UUID,
    CONSTRAINT fk_person_history_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_history_operation CHECK (operation IN
        ('CREATE', 'UPDATE', 'SOFT_DELETE', 'RESTORE', 'MERGE', 'ROLE_ENABLED', 'ROLE_DISABLED'))
);
CREATE INDEX idx_person_history_person ON person_history (company_id, person_id, occurred_at DESC);

CREATE TABLE person_migration_issues (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    person_id      UUID NOT NULL,
    issue_type     VARCHAR(30) NOT NULL,
    original_value TEXT,
    kept_person_id UUID,
    resolved_at    TIMESTAMPTZ,
    resolved_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_person_migration_issues_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_person_migration_issues_type CHECK (issue_type IN
        ('DUPLICATE_SAME_PERSON', 'DUPLICATE_OTHER_PERSON', 'INVALID_TAX_ID'))
);
CREATE INDEX idx_person_migration_issues_open ON person_migration_issues (company_id)
    WHERE resolved_at IS NULL;

-- ===========================================================================
-- 4. Papéis
-- ===========================================================================
CREATE TABLE customers (
    person_id      UUID PRIMARY KEY,
    company_id     UUID NOT NULL,
    customer_code  VARCHAR(20),
    credit_limit   NUMERIC(14, 2),
    blocked        BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason   TEXT,
    customer_since DATE NOT NULL DEFAULT CURRENT_DATE,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_customers_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT uq_customers_company_person UNIQUE (company_id, person_id)
);
CREATE UNIQUE INDEX uq_customers_code ON customers (company_id, customer_code) WHERE customer_code IS NOT NULL;

CREATE TABLE patients (
    person_id          UUID PRIMARY KEY,
    company_id         UUID NOT NULL,
    record_number      VARCHAR(40),
    health_plan        VARCHAR(80),
    health_plan_number VARCHAR(40),
    referral_source    VARCHAR(80),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_patients_customer FOREIGN KEY (company_id, person_id)
        REFERENCES customers (company_id, person_id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX uq_patients_record_number ON patients (company_id, record_number)
    WHERE record_number IS NOT NULL;

CREATE TABLE suppliers (
    person_id          UUID PRIMARY KEY,
    company_id         UUID NOT NULL,
    supplier_code      VARCHAR(20),
    supplier_type      VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    payment_terms_days SMALLINT,
    blocked            BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason       TEXT,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    notes              TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_suppliers_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT uq_suppliers_company_person UNIQUE (company_id, person_id),
    CONSTRAINT ck_suppliers_type CHECK (supplier_type IN ('LAB', 'MATERIAL', 'SERVICE', 'OTHER')),
    CONSTRAINT ck_suppliers_terms CHECK (payment_terms_days IS NULL OR payment_terms_days >= 0)
);
CREATE UNIQUE INDEX uq_suppliers_code ON suppliers (company_id, supplier_code) WHERE supplier_code IS NOT NULL;

CREATE TABLE job_titles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title      VARCHAR(80) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_titles_company_id UNIQUE (company_id, id),
    CONSTRAINT ck_job_titles_title CHECK (length(trim(title)) > 0)
);
CREATE UNIQUE INDEX uq_job_titles_title ON job_titles (company_id, lower(title));

CREATE TABLE employees (
    person_id           UUID PRIMARY KEY,
    company_id          UUID NOT NULL,
    registration_number VARCHAR(20),
    job_title_id        UUID,
    hire_date           DATE,
    termination_date    DATE,
    contract_type       VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    salary              NUMERIC(14, 2),
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_employees_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_employees_job_title FOREIGN KEY (company_id, job_title_id)
        REFERENCES job_titles (company_id, id),
    CONSTRAINT uq_employees_company_person UNIQUE (company_id, person_id),
    CONSTRAINT ck_employees_contract CHECK (contract_type IS NULL OR contract_type IN ('CLT', 'PJ', 'INTERN', 'OTHER')),
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED')),
    CONSTRAINT ck_employees_dates CHECK (termination_date IS NULL OR hire_date IS NULL OR termination_date >= hire_date)
);
CREATE UNIQUE INDEX uq_employees_registration ON employees (company_id, registration_number)
    WHERE registration_number IS NOT NULL;
CREATE UNIQUE INDEX uq_employees_user ON employees (company_id, user_id) WHERE user_id IS NOT NULL;

CREATE TABLE employee_job_history (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID NOT NULL,
    employee_id  UUID NOT NULL,
    job_title_id UUID NOT NULL,
    start_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date     DATE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_employee_job_history_employee FOREIGN KEY (company_id, employee_id)
        REFERENCES employees (company_id, person_id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_job_history_title FOREIGN KEY (company_id, job_title_id)
        REFERENCES job_titles (company_id, id)
);
CREATE INDEX idx_employee_job_history_employee ON employee_job_history (company_id, employee_id);

CREATE TABLE employee_specialties (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL,
    employee_id    UUID NOT NULL,
    specialty      VARCHAR(30) NOT NULL,
    council        VARCHAR(10),
    council_number VARCHAR(20),
    council_uf     CHAR(2),
    is_primary     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_employee_specialties_employee FOREIGN KEY (company_id, employee_id)
        REFERENCES employees (company_id, person_id) ON DELETE CASCADE,
    CONSTRAINT ck_employee_specialties_specialty CHECK (specialty IN
        ('GENERIC', 'DENTAL', 'TRICHOLOGY', 'OCCUPATIONAL_SAFETY')),
    CONSTRAINT uq_employee_specialty UNIQUE (company_id, employee_id, specialty)
);

-- ===========================================================================
-- 5. RLS e GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['persons', 'person_addresses', 'person_contacts', 'person_documents',
                             'person_bank_accounts', 'person_relationships', 'person_history',
                             'person_migration_issues', 'customers', 'patients', 'suppliers',
                             'job_titles', 'employees', 'employee_job_history', 'employee_specialties']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) '
                       'WITH CHECK (company_id = app.current_tenant_id())', t);
        IF t = 'person_history' THEN
            EXECUTE format('GRANT SELECT, INSERT ON %I TO crm_app', t);
        ELSE
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO crm_app', t);
        END IF;
    END LOOP;
END $$;

-- ===========================================================================
-- 6. Sincronização contacts -> persons (transição, até a etapa 8)
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.person_sync_contact_value(p_company UUID, p_person UUID, p_slot TEXT,
                                                         p_type TEXT, p_value TEXT, p_whatsapp BOOLEAN)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF p_value IS NULL OR length(trim(p_value)) = 0 THEN
        DELETE FROM person_contacts WHERE person_id = p_person AND sync_slot = p_slot;
        RETURN;
    END IF;
    UPDATE person_contacts SET value = p_value, contact_type = p_type, updated_at = now()
     WHERE person_id = p_person AND sync_slot = p_slot
       AND (value IS DISTINCT FROM p_value OR contact_type IS DISTINCT FROM p_type);
    IF NOT EXISTS (SELECT 1 FROM person_contacts WHERE person_id = p_person AND sync_slot = p_slot) THEN
        INSERT INTO person_contacts (company_id, person_id, contact_type, value, is_whatsapp, is_primary, sync_slot)
        VALUES (p_company, p_person, p_type, p_value, p_whatsapp,
                NOT EXISTS (SELECT 1 FROM person_contacts
                             WHERE person_id = p_person AND contact_type = p_type AND is_primary AND active),
                p_slot);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION app.person_open_issue(p_company UUID, p_person UUID, p_type TEXT,
                                                 p_original TEXT, p_kept UUID)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM person_migration_issues
                    WHERE person_id = p_person AND issue_type = p_type AND resolved_at IS NULL) THEN
        INSERT INTO person_migration_issues (company_id, person_id, issue_type, original_value, kept_person_id)
        VALUES (p_company, p_person, p_type, p_original, p_kept);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION app.sync_person_from_contact()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_name     TEXT;
    v_doc      TEXT;
    v_tax      TEXT;
    v_type     CHAR(1);
    v_invalid  BOOLEAN := FALSE;
    v_conflict UUID;
    v_phone    TEXT;
    v_mobile   TEXT;
    v_email    TEXT;
    v_zip      TEXT;
    v_code     TEXT;
BEGIN
    v_name := left(trim(coalesce(NEW.first_name, '') || ' ' || coalesce(NEW.last_name, '')), 150);
    IF v_name = '' THEN v_name := 'Sem nome'; END IF;

    v_doc := upper(regexp_replace(coalesce(NEW.cpf, ''), '[^0-9A-Za-z]', '', 'g'));
    IF v_doc <> '' THEN
        IF length(v_doc) = 11 AND app.is_valid_cpf(v_doc) THEN
            v_tax := v_doc; v_type := 'F';
        ELSIF length(v_doc) = 14 AND app.is_valid_cnpj(v_doc) THEN
            v_tax := v_doc; v_type := 'J';
        ELSE
            v_invalid := TRUE;
        END IF;
    END IF;
    IF v_tax IS NOT NULL AND NEW.deleted_at IS NULL THEN
        SELECT p.id INTO v_conflict FROM persons p
         WHERE p.company_id = NEW.company_id AND p.tax_id = v_tax AND NOT p.deleted AND p.id <> NEW.id
         LIMIT 1;
        IF v_conflict IS NOT NULL THEN
            v_tax := NULL;
        END IF;
    END IF;

    INSERT INTO persons AS p (id, company_id, person_type, name, tax_id, rg_or_ie, rg_issuer,
                              birth_or_foundation_date, gender, marital_status, professional_status,
                              provisional, notes, avatar_url, deleted, deleted_at, created_at, updated_at)
    VALUES (NEW.id, NEW.company_id, coalesce(v_type, 'F'), v_name, v_tax, NEW.rg, NEW.rg_issuer,
            NEW.birth_date, NEW.gender, NEW.marital_status, NEW.professional_status,
            (v_doc <> '' AND v_tax IS NULL), NEW.notes, NEW.avatar_url,
            NEW.deleted_at IS NOT NULL, NEW.deleted_at, coalesce(NEW.created_at, now()), now())
    ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name,
        tax_id = EXCLUDED.tax_id,
        person_type = CASE WHEN EXCLUDED.tax_id IS NULL THEN p.person_type ELSE EXCLUDED.person_type END,
        rg_or_ie = EXCLUDED.rg_or_ie,
        rg_issuer = EXCLUDED.rg_issuer,
        birth_or_foundation_date = EXCLUDED.birth_or_foundation_date,
        gender = EXCLUDED.gender,
        marital_status = EXCLUDED.marital_status,
        professional_status = EXCLUDED.professional_status,
        provisional = CASE WHEN EXCLUDED.tax_id IS NOT NULL THEN FALSE
                           ELSE p.provisional OR EXCLUDED.provisional END,
        notes = EXCLUDED.notes,
        avatar_url = EXCLUDED.avatar_url,
        deleted = EXCLUDED.deleted,
        deleted_at = EXCLUDED.deleted_at,
        updated_at = now()
    WHERE (p.name, p.tax_id, p.rg_or_ie, p.rg_issuer, p.birth_or_foundation_date, p.gender, p.marital_status,
           p.professional_status, p.notes, p.avatar_url, p.deleted, p.deleted_at)
          IS DISTINCT FROM
          (EXCLUDED.name, EXCLUDED.tax_id, EXCLUDED.rg_or_ie, EXCLUDED.rg_issuer, EXCLUDED.birth_or_foundation_date,
           EXCLUDED.gender, EXCLUDED.marital_status, EXCLUDED.professional_status, EXCLUDED.notes,
           EXCLUDED.avatar_url, EXCLUDED.deleted, EXCLUDED.deleted_at)
       OR (EXCLUDED.provisional AND NOT p.provisional);

    IF v_invalid THEN
        PERFORM app.person_open_issue(NEW.company_id, NEW.id, 'INVALID_TAX_ID', NEW.cpf, NULL);
    ELSIF v_conflict IS NOT NULL THEN
        PERFORM app.person_open_issue(NEW.company_id, NEW.id, 'DUPLICATE_OTHER_PERSON', v_doc, v_conflict);
    END IF;

    -- Telefone do CRM (WhatsApp), celular e e-mail.
    v_phone  := nullif(regexp_replace(coalesce(NEW.phone, ''), '\D', '', 'g'), '');
    v_mobile := nullif(regexp_replace(coalesce(NEW.mobile, ''), '\D', '', 'g'), '');
    v_email  := nullif(lower(trim(coalesce(NEW.email, ''))), '');
    PERFORM app.person_sync_contact_value(NEW.company_id, NEW.id, 'CONTACT_PHONE', 'MOBILE', v_phone, TRUE);
    PERFORM app.person_sync_contact_value(NEW.company_id, NEW.id, 'CONTACT_MOBILE', 'MOBILE',
        CASE WHEN v_mobile = v_phone THEN NULL ELSE v_mobile END, FALSE);
    PERFORM app.person_sync_contact_value(NEW.company_id, NEW.id, 'CONTACT_EMAIL', 'EMAIL', v_email, FALSE);

    -- Endereço (uma linha residencial).
    v_zip := nullif(regexp_replace(coalesce(NEW.zip_code, ''), '\D', '', 'g'), '');
    IF v_zip IS NOT NULL AND length(v_zip) <> 8 THEN v_zip := NULL; END IF;
    IF coalesce(v_zip, NEW.street, NEW.address_number, NEW.address_complement, NEW.neighborhood,
                NEW.city, NEW.state) IS NULL THEN
        DELETE FROM person_addresses WHERE person_id = NEW.id AND sync_slot = 'CONTACT_ADDRESS';
    ELSE
        UPDATE person_addresses
           SET postal_code = v_zip, street = NEW.street, address_number = NEW.address_number,
               complement = left(NEW.address_complement, 80), neighborhood_name = NEW.neighborhood,
               city_name = NEW.city, uf = upper(left(NEW.state, 2)), updated_at = now()
         WHERE person_id = NEW.id AND sync_slot = 'CONTACT_ADDRESS'
           AND (postal_code, street, address_number, complement, neighborhood_name, city_name, uf)
               IS DISTINCT FROM (v_zip, NEW.street, NEW.address_number, left(NEW.address_complement, 80),
                                 NEW.neighborhood, NEW.city, upper(left(NEW.state, 2)));
        IF NOT EXISTS (SELECT 1 FROM person_addresses WHERE person_id = NEW.id AND sync_slot = 'CONTACT_ADDRESS') THEN
            INSERT INTO person_addresses (company_id, person_id, address_type, postal_code, street, address_number,
                                          complement, neighborhood_name, city_name, uf, is_primary, sync_slot)
            VALUES (NEW.company_id, NEW.id, 'RESIDENTIAL', v_zip, NEW.street, NEW.address_number,
                    left(NEW.address_complement, 80), NEW.neighborhood, NEW.city, upper(left(NEW.state, 2)),
                    NOT EXISTS (SELECT 1 FROM person_addresses
                                 WHERE person_id = NEW.id AND address_type = 'RESIDENTIAL' AND is_primary AND active),
                    'CONTACT_ADDRESS');
        END IF;
    END IF;

    -- Cliente/paciente.
    IF NEW.is_patient THEN
        IF NOT EXISTS (SELECT 1 FROM customers WHERE person_id = NEW.id) THEN
            SELECT (coalesce(max(customer_code::INT), 0) + 1)::TEXT INTO v_code
              FROM customers WHERE company_id = NEW.company_id AND customer_code ~ '^[0-9]{1,9}$';
            INSERT INTO customers (person_id, company_id, customer_code, customer_since)
            VALUES (NEW.id, NEW.company_id, v_code, coalesce(NEW.patient_since, NEW.created_at::DATE, CURRENT_DATE));
        END IF;
        INSERT INTO patients AS pt (person_id, company_id, record_number, health_plan, health_plan_number, referral_source)
        VALUES (NEW.id, NEW.company_id, NEW.record_number, NEW.health_plan, NEW.health_plan_number, NEW.referral_source)
        ON CONFLICT (person_id) DO UPDATE SET
            record_number = EXCLUDED.record_number,
            health_plan = EXCLUDED.health_plan,
            health_plan_number = EXCLUDED.health_plan_number,
            referral_source = EXCLUDED.referral_source,
            updated_at = now()
        WHERE (pt.record_number, pt.health_plan, pt.health_plan_number, pt.referral_source)
              IS DISTINCT FROM (EXCLUDED.record_number, EXCLUDED.health_plan, EXCLUDED.health_plan_number,
                                EXCLUDED.referral_source);
    ELSE
        DELETE FROM patients WHERE person_id = NEW.id;
    END IF;

    RETURN NEW;
END $$;

CREATE TRIGGER trg_contacts_sync_person
    BEFORE INSERT OR UPDATE ON contacts
    FOR EACH ROW EXECUTE FUNCTION app.sync_person_from_contact();

-- ===========================================================================
-- 7. Backfill (por empresa: contacts tem RLS FORCE). O UPDATE "vazio" dispara o gatilho;
--    a ordem de criação garante que o cadastro mais antigo fique com o CPF.
-- ===========================================================================
DO $$
DECLARE c RECORD; k RECORD; n INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::TEXT, TRUE);
        n := 0;
        FOR k IN SELECT id FROM contacts WHERE company_id = c.id
                  ORDER BY (deleted_at IS NOT NULL), created_at, id LOOP
            UPDATE contacts SET updated_at = updated_at WHERE id = k.id;
            n := n + 1;
        END LOOP;
        IF n > 0 THEN
            RAISE NOTICE 'V147: % contatos sincronizados para a empresa %', n, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', '', TRUE);
END $$;

-- ===========================================================================
-- 8. contacts passa a ser a parte de CRM da pessoa (mesmo UUID)
-- ===========================================================================
ALTER TABLE contacts ADD CONSTRAINT uq_contacts_company_id UNIQUE (company_id, id);
ALTER TABLE contacts ADD CONSTRAINT fk_contacts_person
    FOREIGN KEY (company_id, id) REFERENCES persons (company_id, id);

-- ===========================================================================
-- 9. Permissões
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('person:read',     'Read persons',                         'people', 'person',   'read'),
    ('person:create',   'Create persons',                       'people', 'person',   'create'),
    ('person:update',   'Update persons and customer role',     'people', 'person',   'update'),
    ('person:delete',   'Soft delete persons',                  'people', 'person',   'delete'),
    ('person:sensitive','View full CPF/CNPJ and documents',     'people', 'person',   'sensitive'),
    ('person:bank',     'View and edit bank accounts and PIX',  'people', 'person',   'bank'),
    ('person:merge',    'Merge duplicated persons',             'people', 'person',   'merge'),
    ('supplier:manage', 'Manage suppliers',                     'people', 'supplier', 'manage'),
    ('employee:manage', 'Manage employees and job titles',      'people', 'employee', 'manage'),
    ('employee:salary', 'View and edit employee salary',        'people', 'employee', 'salary'),
    ('location:manage', 'Manage countries, states, cities and neighborhoods', 'people', 'location', 'manage')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::TEXT, FALSE);
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
         WHERE r.company_id = c.id AND (
               (r.name = 'CRM_ADMIN' AND p.module = 'people')
            OR (r.name = 'ADMIN' AND p.module = 'people')
            OR (r.name = 'MANAGER' AND p.name IN ('person:read', 'person:create', 'person:update', 'person:delete',
                                                 'person:sensitive', 'person:merge', 'supplier:manage',
                                                 'employee:manage', 'location:manage'))
            OR (r.name = 'AGENT' AND p.name IN ('person:read', 'person:create', 'person:update', 'person:sensitive'))
            OR (r.name = 'VIEWER' AND p.name IN ('person:read')))
        ON CONFLICT (role_id, permission_id) DO NOTHING;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::TEXT, FALSE);
END $$;

-- ===========================================================================
-- 10. Módulos: Pessoas (com Clientes), Fornecedores, Funcionários, Localidades
-- ===========================================================================
UPDATE companies
   SET enabled_modules = enabled_modules || ',PEOPLE,SUPPLIERS,EMPLOYEES,LOCATIONS'
 WHERE (',' || enabled_modules || ',') NOT LIKE '%,PEOPLE,%';

ALTER TABLE companies
    ALTER COLUMN enabled_modules SET DEFAULT 'CRM,PATIENTS,AGENDA,FINANCE,TASKS,ASSISTANT,INBOX,FOLLOW_UPS,CAMPAIGNS,WORKFLOWS,CATALOG,INVENTORY,REPORTS,STORAGE,AGENDA_SETTINGS,EMAIL_SETTINGS,AI_AGENT,DOCUMENTS,DATA_MIGRATION,ROLES,AUDIT,CREDIT_SIMULATOR,CARD_FEES,PATIENT_BILLING,WHATSAPP,TELEGRAM,PEOPLE,SUPPLIERS,EMPLOYEES,LOCATIONS';
