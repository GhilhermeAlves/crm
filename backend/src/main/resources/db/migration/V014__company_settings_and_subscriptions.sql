-- V014__company_settings_and_subscriptions.sql
-- Company settings and subscription tables

CREATE TABLE company_settings (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    timezone                 VARCHAR(50)  NOT NULL DEFAULT 'America/Sao_Paulo',
    locale                   VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    currency                 VARCHAR(10)  NOT NULL DEFAULT 'BRL',
    business_hours           TEXT         NULL,
    notification_preferences TEXT         NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriptions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    plan                     VARCHAR(20)  NOT NULL DEFAULT 'STARTER',
    status                   VARCHAR(20)  NOT NULL DEFAULT 'TRIAL',
    start_date               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date                 TIMESTAMP    NOT NULL,
    max_users                INTEGER      NOT NULL DEFAULT 5,
    max_contacts             INTEGER      NOT NULL DEFAULT 500,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_settings_company_id ON company_settings (company_id);
CREATE INDEX idx_subscriptions_company_id ON subscriptions (company_id);
