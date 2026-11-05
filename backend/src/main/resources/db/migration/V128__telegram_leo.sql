-- V128__telegram_leo.sql
-- Leo pelo Telegram: bot por clínica, vínculo usuário↔Telegram, códigos de
-- vínculo e idempotência de updates. Token cifrado (AES-GCM) pela aplicação.

CREATE TABLE telegram_bot_config (
    company_id          UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    bot_token_cipher    TEXT NOT NULL,
    webhook_secret_hash VARCHAR(64) NOT NULL,
    bot_username        VARCHAR(64) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT true,
    updated_by          UUID,
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE telegram_link (
    id                 UUID PRIMARY KEY,
    company_id         UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    telegram_user_id   BIGINT NOT NULL,
    chat_id            BIGINT NOT NULL,
    telegram_name      VARCHAR(255),
    ai_conversation_id UUID,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_telegram_link_tg UNIQUE (company_id, telegram_user_id),
    CONSTRAINT uq_telegram_link_user UNIQUE (company_id, user_id)
);

CREATE TABLE telegram_link_code (
    code_hash  VARCHAR(64) PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP
);
CREATE INDEX idx_telegram_link_code_user ON telegram_link_code(company_id, user_id);

CREATE TABLE telegram_update_seen (
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    update_id  BIGINT NOT NULL,
    seen_at    TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, update_id)
);

ALTER TABLE telegram_bot_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_bot_config FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_bot_config
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_bot_config TO crm_app;

ALTER TABLE telegram_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_link FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_link
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_link TO crm_app;

ALTER TABLE telegram_link_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_link_code FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_link_code
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_link_code TO crm_app;

ALTER TABLE telegram_update_seen ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_update_seen FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_update_seen
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_update_seen TO crm_app;
