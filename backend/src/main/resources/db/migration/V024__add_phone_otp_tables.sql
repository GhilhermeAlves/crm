-- V024__add_phone_otp_tables.sql
-- Sprint 7.3 — Telefone/OTP
-- Adiciona tabela de códigos OTP e campos de verificação de telefone

-- Tabela de códigos OTP
CREATE TABLE IF NOT EXISTS otp_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_e164 VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_otp_codes_phone_created ON otp_codes(phone_e164, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_otp_codes_expires ON otp_codes(expires_at);

-- Campos de verificação de telefone na tabela users
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP;

COMMENT ON COLUMN users.phone_verified IS 'Indica se o telefone foi verificado via OTP (Sprint 7.3)';
COMMENT ON COLUMN users.phone_verified_at IS 'Timestamp da verificação do telefone via OTP';
COMMENT ON TABLE otp_codes IS 'Códigos OTP temporários para verificação de telefone (Sprint 7.3)';