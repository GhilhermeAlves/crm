-- V118__companies_logo_text.sql
-- O logo da empresa passa a ser guardado como data URL (PNG/JPG/SVG), que não
-- cabe em VARCHAR(512). Usado no topo do formulário público do paciente.

ALTER TABLE companies ALTER COLUMN logo_url TYPE TEXT;
