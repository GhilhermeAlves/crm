-- V157__report_exports_row_count.sql
-- Quantidade de linhas do arquivo gerado (preenchida ao ficar READY). A
-- auditoria do ciclo de exportação reutiliza audit_logs (módulo REPORTS), sem
-- tabela nova; RLS/GRANT de report_exports não mudam (V096).

ALTER TABLE report_exports ADD COLUMN IF NOT EXISTS row_count INTEGER;
