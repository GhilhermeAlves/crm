-- V086__agent_scheduling_and_voice.sql
-- A) Agendamentos criados pelo agente de WhatsApp ganham origem própria.
-- B) Resposta em áudio do agente: NEVER | MIRROR (só quando o paciente manda
--    áudio) | ALWAYS.

ALTER TABLE appointments DROP CONSTRAINT chk_appt_source;
ALTER TABLE appointments
    ADD CONSTRAINT chk_appt_source CHECK (source IN ('INTERNAL', 'PUBLIC_LINK', 'WHATSAPP'));

ALTER TABLE agent_config
    ADD COLUMN voice_reply_mode VARCHAR(10) NOT NULL DEFAULT 'MIRROR';
ALTER TABLE agent_config
    ADD CONSTRAINT chk_agent_config_voice_reply_mode CHECK (voice_reply_mode IN ('NEVER', 'MIRROR', 'ALWAYS'));
