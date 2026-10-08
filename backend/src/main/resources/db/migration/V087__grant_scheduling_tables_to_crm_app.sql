-- V087__grant_scheduling_tables_to_crm_app.sql
-- V079 (agenda) e V082 (mensagem de aniversário) criaram tabelas sem o GRANT
-- para o papel da aplicação (padrão V050/V070): em produção toda leitura e
-- escrita falhava com "permission denied" (ex.: criar tipo de agendamento → 400).

GRANT SELECT, INSERT, UPDATE, DELETE ON appointment_types TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON appointment_type_hosts TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_scheduling_settings TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON availability_rules TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON availability_overrides TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON appointments TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON schedule_blocks TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON birthday_message_settings TO crm_app;
