-- V144__company_modules_finance_channels.sql
-- Funções que estavam dentro de outro módulo viram módulos próprios.
-- Empresas existentes recebem o novo módulo quando já usam o módulo de origem,
-- para ninguém perder acesso: FINANCE -> CREDIT_SIMULATOR, CARD_FEES,
-- PATIENT_BILLING; INBOX -> WHATSAPP; AI_AGENT -> TELEGRAM.

UPDATE companies
SET enabled_modules = enabled_modules || ',CREDIT_SIMULATOR,CARD_FEES,PATIENT_BILLING'
WHERE (',' || enabled_modules || ',') LIKE '%,FINANCE,%';

UPDATE companies
SET enabled_modules = enabled_modules || ',WHATSAPP'
WHERE (',' || enabled_modules || ',') LIKE '%,INBOX,%';

UPDATE companies
SET enabled_modules = enabled_modules || ',TELEGRAM'
WHERE (',' || enabled_modules || ',') LIKE '%,AI_AGENT,%';

ALTER TABLE companies
    ALTER COLUMN enabled_modules SET DEFAULT 'CRM,PATIENTS,AGENDA,FINANCE,TASKS,ASSISTANT,INBOX,FOLLOW_UPS,CAMPAIGNS,WORKFLOWS,CATALOG,INVENTORY,REPORTS,STORAGE,AGENDA_SETTINGS,EMAIL_SETTINGS,AI_AGENT,DOCUMENTS,DATA_MIGRATION,ROLES,AUDIT,CREDIT_SIMULATOR,CARD_FEES,PATIENT_BILLING,WHATSAPP,TELEGRAM';
