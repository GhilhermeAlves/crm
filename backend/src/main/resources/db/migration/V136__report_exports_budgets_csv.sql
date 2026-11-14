-- V136__report_exports_budgets_csv.sql
-- Relatório de orçamentos (BUDGETS) e saída em CSV nas exportações de relatório.

ALTER TABLE report_exports DROP CONSTRAINT IF EXISTS ck_report_exports_type;
ALTER TABLE report_exports ADD CONSTRAINT ck_report_exports_type
    CHECK (type IN ('MONTHLY_DRE', 'CASH_FLOW', 'TRANSACTIONS', 'AGENDA', 'BUDGETS'));

ALTER TABLE report_exports DROP CONSTRAINT IF EXISTS ck_report_exports_format;
ALTER TABLE report_exports ADD CONSTRAINT ck_report_exports_format
    CHECK (format IN ('PDF', 'XLSX', 'CSV'));
