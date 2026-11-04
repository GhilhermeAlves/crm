-- V127__company_mail_invitation_template.sql
-- Modelo do e-mail de convite de cada empresa (Configurações > E-mail).
-- Fica junto da conta de envio: só há convite pela conta da empresa.
-- O passo a passo e o link do convite continuam gerados pelo sistema.
--   invite_enabled  usa o modelo personalizado (false = modelo padrão)
--   invite_logo     data URL PNG/JPEG (até ~500 KB), anexado inline no e-mail
--   invite_color    cor principal #RRGGBB
-- RLS e GRANT da tabela (V116) já cobrem as colunas novas.

ALTER TABLE company_mail_settings
    ADD COLUMN invite_enabled BOOLEAN      NOT NULL DEFAULT false,
    ADD COLUMN invite_logo    TEXT,
    ADD COLUMN invite_color   VARCHAR(7),
    ADD COLUMN invite_title   VARCHAR(150),
    ADD COLUMN invite_message VARCHAR(1500),
    ADD COLUMN invite_footer  VARCHAR(200);
