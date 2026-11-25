-- V149__person_field_settings.sql
-- Obrigatoriedade de campos do cadastro de pessoas por empresa (doc seção 18).
-- Catálogo global (person_fields + person_field_contexts com o padrão) e exceções por
-- empresa (company_person_field_settings). O valor efetivo é a exceção da empresa ou, sem
-- ela, o padrão do catálogo (RNP011 sem precisar gerar linhas para cada empresa nova).
-- Empresas existentes recebem exceções que preservam o comportamento atual: nada novo
-- obrigatório para clientes, e CPF de cliente conforme company_settings.require_contact_cpf.

CREATE TABLE person_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module          VARCHAR(30) NOT NULL DEFAULT 'PERSON',
    group_name      VARCHAR(40) NOT NULL,
    field_key       VARCHAR(60) NOT NULL,
    label           VARCHAR(80) NOT NULL,
    system_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_person_fields_key UNIQUE (field_key),
    CONSTRAINT ck_person_fields_group CHECK (group_name IN
        ('IDENTIFICATION', 'ADDRESS', 'CONTACT', 'COMPLEMENTARY', 'PATIENT', 'PROFESSIONAL'))
);

CREATE TABLE person_field_contexts (
    field_id         UUID NOT NULL REFERENCES person_fields (id),
    context          VARCHAR(20) NOT NULL,
    default_required BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_person_field_contexts PRIMARY KEY (field_id, context),
    CONSTRAINT ck_person_field_contexts CHECK (context IN ('PF', 'PJ', 'CUSTOMER', 'SUPPLIER', 'EMPLOYEE'))
);

CREATE TABLE company_person_field_settings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    field_id   UUID NOT NULL,
    context    VARCHAR(20) NOT NULL,
    required   BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    version    INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_company_field_settings_context FOREIGN KEY (field_id, context)
        REFERENCES person_field_contexts (field_id, context),
    CONSTRAINT uq_company_field_settings UNIQUE (company_id, field_id, context)
);

GRANT SELECT ON person_fields, person_field_contexts TO crm_app;
ALTER TABLE company_person_field_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_person_field_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON company_person_field_settings
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON company_person_field_settings TO crm_app;

-- Catálogo: chave, grupo, rótulo, obrigatório pelo sistema, ordem e padrão por contexto
-- (NULL = não se aplica). Ordem dos contextos: PF, PJ, CUSTOMER, SUPPLIER, EMPLOYEE.
WITH t (k, g, l, s, o, pf, pj, cu, su, em) AS (VALUES
    ('PERSON_NAME',            'IDENTIFICATION', 'Nome / Razão social',              TRUE,  1, TRUE,  TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_TAX_ID',          'IDENTIFICATION', 'CPF / CNPJ',                       FALSE, 2, FALSE, TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_RG_IE',           'IDENTIFICATION', 'RG / Inscrição estadual',          FALSE, 3, FALSE, FALSE, FALSE, TRUE,  TRUE),
    ('PERSON_BIRTH_DATE',      'IDENTIFICATION', 'Data de nascimento / abertura',    FALSE, 4, FALSE, FALSE, FALSE, FALSE, TRUE),
    ('PERSON_TRADE_NAME',      'IDENTIFICATION', 'Nome fantasia',                    FALSE, 5, NULL,  FALSE, FALSE, FALSE, NULL),
    ('PERSON_POSTAL_CODE',     'ADDRESS',        'CEP',                              FALSE, 1, FALSE, TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_STREET',          'ADDRESS',        'Logradouro',                       FALSE, 2, FALSE, TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_ADDRESS_NUMBER',  'ADDRESS',        'Número',                           FALSE, 3, FALSE, TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_NEIGHBORHOOD',    'ADDRESS',        'Bairro',                           FALSE, 4, FALSE, FALSE, TRUE,  TRUE,  TRUE),
    ('PERSON_CITY',            'ADDRESS',        'Cidade',                           FALSE, 5, FALSE, TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_STATE',           'ADDRESS',        'Estado',                           FALSE, 6, FALSE, TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_COUNTRY',         'ADDRESS',        'País',                             FALSE, 7, TRUE,  TRUE,  TRUE,  TRUE,  TRUE),
    ('PERSON_PHONE',           'CONTACT',        'Telefone',                         FALSE, 1, FALSE, FALSE, FALSE, TRUE,  FALSE),
    ('PERSON_MOBILE',          'CONTACT',        'Celular',                          FALSE, 2, FALSE, FALSE, TRUE,  FALSE, TRUE),
    ('PERSON_EMAIL',           'CONTACT',        'E-mail',                           FALSE, 3, FALSE, TRUE,  FALSE, TRUE,  TRUE),
    ('PERSON_WHATSAPP',        'CONTACT',        'WhatsApp',                         FALSE, 4, FALSE, FALSE, FALSE, FALSE, FALSE),
    ('EMPLOYEE_JOB_TITLE',     'COMPLEMENTARY',  'Cargo do funcionário',             FALSE, 1, NULL,  NULL,  NULL,  NULL,  TRUE),
    ('EMPLOYEE_HIRE_DATE',     'COMPLEMENTARY',  'Data de admissão',                 FALSE, 2, NULL,  NULL,  NULL,  NULL,  TRUE),
    ('PERSON_BANK_ACCOUNT',    'COMPLEMENTARY',  'Dados bancários',                  FALSE, 3, FALSE, FALSE, FALSE, TRUE,  TRUE),
    ('PERSON_DOCUMENT',        'COMPLEMENTARY',  'Documento adicional',              FALSE, 4, FALSE, FALSE, FALSE, FALSE, FALSE),
    ('PATIENT_RECORD_NUMBER',  'PATIENT',        'Nº do prontuário',                 FALSE, 1, NULL,  NULL,  FALSE, NULL,  NULL),
    ('PATIENT_HEALTH_PLAN',    'PATIENT',        'Convênio / nº da carteirinha',     FALSE, 2, NULL,  NULL,  FALSE, NULL,  NULL),
    ('PATIENT_REFERRAL_SOURCE','PATIENT',        'Como conheceu (origem)',           FALSE, 3, NULL,  NULL,  FALSE, NULL,  NULL),
    ('PATIENT_GUARDIAN_MINOR', 'PATIENT',        'Responsável (cliente menor de idade)', FALSE, 4, NULL, NULL, TRUE, NULL,  NULL),
    ('EMPLOYEE_SPECIALTY',     'PROFESSIONAL',   'Especialidade',                    FALSE, 1, NULL,  NULL,  NULL,  NULL,  FALSE),
    ('EMPLOYEE_COUNCIL',       'PROFESSIONAL',   'Conselho e número (CRO, CRM)',     FALSE, 2, NULL,  NULL,  NULL,  NULL,  FALSE)
),
ins AS (
    INSERT INTO person_fields (group_name, field_key, label, system_required, sort_order)
    SELECT g, k, l, s, o FROM t
    RETURNING id, field_key
)
INSERT INTO person_field_contexts (field_id, context, default_required)
SELECT ins.id, x.ctx, x.req
  FROM t
  JOIN ins ON ins.field_key = t.k
  CROSS JOIN LATERAL (VALUES ('PF', t.pf), ('PJ', t.pj), ('CUSTOMER', t.cu), ('SUPPLIER', t.su), ('EMPLOYEE', t.em))
       AS x(ctx, req)
 WHERE x.req IS NOT NULL;

-- Empresas existentes: preserva o comportamento atual.
DO $$
DECLARE c RECORD; v_cpf BOOLEAN;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::TEXT, TRUE);
        SELECT require_contact_cpf INTO v_cpf FROM company_settings WHERE company_id = c.id;
        INSERT INTO company_person_field_settings (company_id, field_id, context, required)
        SELECT c.id, fc.field_id, fc.context,
               CASE WHEN f.field_key = 'PERSON_TAX_ID' THEN coalesce(v_cpf, FALSE) ELSE FALSE END
          FROM person_field_contexts fc
          JOIN person_fields f ON f.id = fc.field_id
         WHERE fc.context IN ('PF', 'CUSTOMER') AND fc.default_required AND NOT f.system_required;
    END LOOP;
    PERFORM set_config('app.current_company_id', '', TRUE);
END $$;
