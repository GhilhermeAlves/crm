-- V160__report_exports_leads.sql
-- Relatório de leads (LEADS) nas exportações de relatório.

ALTER TABLE report_exports DROP CONSTRAINT IF EXISTS ck_report_exports_type;
ALTER TABLE report_exports ADD CONSTRAINT ck_report_exports_type
    CHECK (type IN ('MONTHLY_DRE', 'CASH_FLOW', 'TRANSACTIONS', 'AGENDA', 'BUDGETS', 'LEADS'));
