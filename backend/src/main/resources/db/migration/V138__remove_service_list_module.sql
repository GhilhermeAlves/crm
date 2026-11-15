-- "Lista de serviços" foi removida: o Catálogo já cadastra e lista os serviços.
-- Tira SERVICE_LIST das empresas, dos convites de empresa e do default da coluna.

UPDATE companies
SET enabled_modules = TRIM(BOTH ',' FROM REPLACE(',' || enabled_modules || ',', ',SERVICE_LIST,', ','))
WHERE (',' || enabled_modules || ',') LIKE '%,SERVICE_LIST,%';

UPDATE company_invitations
SET enabled_modules = TRIM(BOTH ',' FROM REPLACE(',' || enabled_modules || ',', ',SERVICE_LIST,', ','))
WHERE (',' || enabled_modules || ',') LIKE '%,SERVICE_LIST,%';

ALTER TABLE companies
    ALTER COLUMN enabled_modules SET DEFAULT 'CRM,PATIENTS,AGENDA,FINANCE,TASKS,ASSISTANT,INBOX,FOLLOW_UPS,CAMPAIGNS,WORKFLOWS,CATALOG,INVENTORY,REPORTS,STORAGE,AGENDA_SETTINGS,EMAIL_SETTINGS,AI_AGENT,DOCUMENTS,DATA_MIGRATION,ROLES,AUDIT';
