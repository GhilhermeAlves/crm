-- V099__agenda_settings.sql
-- Ajustes da agenda por empresa (Fase D): duração padrão da consulta, horário de
-- trabalho exibido na agenda e quais notificações de agendamento ficam ativas.
-- Padrão V082: uma linha por empresa, RLS FORCE e GRANT para crm_app (V087).
CREATE TABLE agenda_settings (
    company_id               UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    default_duration_minutes INTEGER NOT NULL DEFAULT 30,
    work_hours_start         TIME NOT NULL DEFAULT '08:00',
    work_hours_end           TIME NOT NULL DEFAULT '18:00',
    notify_on_booking        BOOLEAN NOT NULL DEFAULT TRUE,
    notify_24h_before        BOOLEAN NOT NULL DEFAULT TRUE,
    notify_1h_before         BOOLEAN NOT NULL DEFAULT FALSE,
    notify_on_cancel         BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_reschedule     BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_agenda_settings_duration CHECK (default_duration_minutes BETWEEN 5 AND 480),
    CONSTRAINT chk_agenda_settings_work_hours CHECK (work_hours_start < work_hours_end)
);

ALTER TABLE agenda_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE agenda_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON agenda_settings
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON agenda_settings TO crm_app;
