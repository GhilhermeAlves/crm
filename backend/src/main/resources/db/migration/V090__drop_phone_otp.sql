-- V090__drop_phone_otp.sql
-- Remove o login por telefone/OTP (Sprint 7.3/7.4): o acesso passa a ser
-- exclusivamente pela tela de login do Keycloak. users.phone permanece (dado de contato).

DROP POLICY IF EXISTS identity_phone_bootstrap_policy ON users;
DROP POLICY IF EXISTS identity_phone_link_policy ON users;

DROP TABLE IF EXISTS otp_codes;

ALTER TABLE users
    DROP COLUMN IF EXISTS phone_verified,
    DROP COLUMN IF EXISTS phone_verified_at;
