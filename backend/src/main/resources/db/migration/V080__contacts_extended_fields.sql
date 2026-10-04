-- V080__contacts_extended_fields.sql
-- Add personal data fields to contacts (matching Capim patient registration form)

ALTER TABLE contacts
    ADD COLUMN birth_date      DATE,
    ADD COLUMN cpf             VARCHAR(14),
    ADD COLUMN rg              VARCHAR(20),
    ADD COLUMN rg_issuer       VARCHAR(20),
    ADD COLUMN gender          VARCHAR(20),
    ADD COLUMN marital_status  VARCHAR(30),
    ADD COLUMN mobile          VARCHAR(20);
