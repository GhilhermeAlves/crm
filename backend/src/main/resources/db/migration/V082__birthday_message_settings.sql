-- V082__birthday_message_settings.sql
-- Configuração da mensagem automática de aniversário (uma por empresa). Padrão V079: RLS FORCE.
CREATE TABLE birthday_message_settings (
    company_id  UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    template    TEXT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bms_template_len CHECK (char_length(template) BETWEEN 1 AND 2000)
);

ALTER TABLE birthday_message_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE birthday_message_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON birthday_message_settings
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
