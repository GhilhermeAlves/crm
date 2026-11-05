-- V129__notifications_delete_grant.sql
-- O usuário passa a excluir as próprias notificações (sino). A V047 criou a
-- tabela depois da padronização de grants (V034) sem GRANT próprio; garante
-- aqui o padrão SELECT/INSERT/UPDATE/DELETE ao crm_app. RLS continua valendo.
GRANT SELECT, INSERT, UPDATE, DELETE ON notifications TO crm_app;
