-- V154__trichology_document_type_permissions_module.sql
-- 1) Relatório do exame vira documento do paciente (TRICHOLOGY_REPORT).
-- 2) Permissões do módulo e vínculo aos papéis existentes (CRM_ADMIN recebe tudo).
-- 3) Módulo TRICHOLOGY ligado para as empresas do segmento TRICHOLOGY.
-- Roda como crm_admin (superuser), então os UPDATE/INSERT não passam pelo RLS.

ALTER TABLE patient_documents DROP CONSTRAINT chk_patient_documents_type;
ALTER TABLE patient_documents ADD CONSTRAINT chk_patient_documents_type
    CHECK (type IN ('PRESCRIPTION', 'CERTIFICATE', 'CONSENT', 'OTHER', 'TRICHOLOGY_REPORT'));

-- Modelos de documento não usam o tipo novo (o relatório tem renderer próprio).

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('trichology:read',     'View trichology assessments and reports',   'trichology', 'trichology', 'read'),
    ('trichology:write',    'Perform trichology assessments',            'trichology', 'trichology', 'write'),
    ('trichology:approve',  'Approve and publish trichology reports',    'trichology', 'trichology', 'approve'),
    ('trichology:settings', 'Configure trichology protocols and catalogs','trichology', 'trichology', 'settings')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::TEXT, FALSE);
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
         WHERE r.company_id = c.id AND (
               (r.name IN ('CRM_ADMIN', 'ADMIN') AND p.module = 'trichology')
            OR (r.name = 'MANAGER' AND p.name IN ('trichology:read', 'trichology:write', 'trichology:approve'))
            OR (r.name = 'AGENT' AND p.name IN ('trichology:read', 'trichology:write'))
            OR (r.name = 'VIEWER' AND p.name IN ('trichology:read')))
        ON CONFLICT (role_id, permission_id) DO NOTHING;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::TEXT, FALSE);
END $$;

UPDATE companies
   SET enabled_modules = enabled_modules || ',TRICHOLOGY'
 WHERE segment = 'TRICHOLOGY'
   AND (',' || enabled_modules || ',') NOT LIKE '%,TRICHOLOGY,%';
