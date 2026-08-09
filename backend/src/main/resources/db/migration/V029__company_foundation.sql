-- V029__company_foundation.sql
-- Sprint 8.1 - Company Foundation
-- Exposicao do limite max_contacts no modelo de empresas (enforcement ocorre na 8.6).

ALTER TABLE companies
    ADD COLUMN max_contacts INTEGER NOT NULL DEFAULT 500;

COMMENT ON COLUMN companies.max_contacts IS
    'Limite de contatos permitidos pelo plano (enforcement previsto na 8.6).';
