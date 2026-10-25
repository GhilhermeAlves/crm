-- V111__company_modules_per_menu.sql
-- Todo item do menu vira módulo por empresa. Expande o CSV para incluir os novos módulos.
-- Não remove acesso de empresas existentes: acresce os novos módulos às empresas já existentes.

-- Aumenta o tamanho do CSV para comportar todos os módulos
ALTER TABLE companies
    ALTER COLUMN enabled_modules TYPE VARCHAR(500);

-- Adiciona os novos módulos para todas as empresas existentes
-- (mantém os 4 módulos atuais)
UPDATE companies
SET enabled_modules = CASE
    WHEN enabled_modules IS NULL OR TRIM(enabled_modules) = '' THEN
        'CRM,PATIENTS,AGENDA,FINANCE,TASKS,ASSISTANT,INBOX,FOLLOW_UPS,CAMPAIGNS,WORKFLOWS,CATALOG,INVENTORY,REPORTS,STORAGE'
    ELSE
        TRIM(enabled_modules) || ',TASKS,ASSISTANT,INBOX,FOLLOW_UPS,CAMPAIGNS,WORKFLOWS,CATALOG,INVENTORY,REPORTS,STORAGE'
    END;

-- Atualiza o DEFAULT para conter todos os 14 módulos
ALTER TABLE companies
    ALTER COLUMN enabled_modules SET DEFAULT 'CRM,PATIENTS,AGENDA,FINANCE,TASKS,ASSISTANT,INBOX,FOLLOW_UPS,CAMPAIGNS,WORKFLOWS,CATALOG,INVENTORY,REPORTS,STORAGE';
