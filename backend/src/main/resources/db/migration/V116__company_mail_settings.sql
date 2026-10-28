-- V116__company_mail_settings.sql
-- Conta de e-mail da própria clínica (SMTP) usada para enviar formulários ao
-- paciente. A senha fica cifrada (AES-GCM) pela aplicação; nunca volta à tela.

CREATE TABLE company_mail_settings (
    company_id      UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    sender_name     VARCHAR(255),
    host            VARCHAR(255) NOT NULL,
    port            INTEGER NOT NULL,
    password_cipher TEXT NOT NULL,
    updated_by      UUID,
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE company_mail_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_mail_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON company_mail_settings
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON company_mail_settings TO crm_app;
