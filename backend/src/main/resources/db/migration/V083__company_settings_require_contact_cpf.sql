-- V083__company_settings_require_contact_cpf.sql
-- Preferência da empresa: exigir CPF no cadastro e na edição de contatos.
ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS require_contact_cpf BOOLEAN NOT NULL DEFAULT FALSE;
