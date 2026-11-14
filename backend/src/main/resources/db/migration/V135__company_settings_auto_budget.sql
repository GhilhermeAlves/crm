-- V135__company_settings_auto_budget.sql
-- Preferência por empresa: todo procedimento planejado entra automaticamente no
-- orçamento em aberto do paciente, e o lançamento financeiro (a receber) é
-- criado quando o procedimento é realizado. Desligada por padrão.

ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS auto_budget_from_procedures BOOLEAN NOT NULL DEFAULT FALSE;
