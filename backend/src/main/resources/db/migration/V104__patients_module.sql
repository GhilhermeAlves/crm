-- Módulo Pacientes + módulos por empresa.
--
-- 1. contacts.is_patient / patient_since: paciente continua sendo uma linha de
--    contacts (mesmo telefone/WhatsApp/histórico), apenas marcada. Backfill:
--    contatos com nº de prontuário, consulta ou lançamento financeiro.
-- 2. companies.segment / enabled_modules: segmento da empresa e módulos
--    habilitados (CSV de CRM, PATIENTS, AGENDA, FINANCE), alterados só pelo
--    SUPER_ADMIN. As empresas atuais são clínicas: entram como DENTAL com tudo.
--
-- Só ALTERs em tabelas existentes (RLS/GRANT já aplicados em contacts e
-- companies); nenhuma tabela nova.

-- ===========================================================================
-- 1. contacts — marcação de paciente
-- ===========================================================================
ALTER TABLE contacts
    ADD COLUMN is_patient    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN patient_since DATE;

-- contacts/appointments/financial_transactions têm RLS FORCE: sem o GUC de
-- tenant o UPDATE não enxergaria nenhuma linha quando a migração não roda como
-- superuser. Por isso o backfill percorre as empresas (companies não tem RLS)
-- definindo o tenant de cada uma, apenas nesta transação.
DO $$
DECLARE
    cid UUID;
BEGIN
    FOR cid IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', cid::text, TRUE);
        UPDATE contacts c
           SET is_patient = TRUE,
               patient_since = c.created_at::date
         WHERE c.company_id = cid
           AND (c.record_number IS NOT NULL
                OR EXISTS (SELECT 1 FROM appointments a WHERE a.contact_id = c.id)
                OR EXISTS (SELECT 1 FROM financial_transactions f WHERE f.contact_id = c.id));
    END LOOP;
    PERFORM set_config('app.current_company_id', '', TRUE);
END $$;

CREATE INDEX idx_contacts_company_is_patient ON contacts (company_id, is_patient);

-- ===========================================================================
-- 2. companies — segmento e módulos habilitados
-- ===========================================================================
ALTER TABLE companies
    ADD COLUMN segment         VARCHAR(30)  NOT NULL DEFAULT 'GENERIC',
    ADD COLUMN enabled_modules VARCHAR(200) NOT NULL DEFAULT 'CRM';

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_segment
        CHECK (segment IN ('GENERIC', 'DENTAL', 'OCCUPATIONAL_SAFETY'));

UPDATE companies
   SET segment = 'DENTAL',
       enabled_modules = 'CRM,PATIENTS,AGENDA,FINANCE';
