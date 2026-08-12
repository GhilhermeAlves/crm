-- V035__lockdown_reference_table_grants.sql
-- Governança (Parte 1.2): remove privilégios de escrita de crm_app em tabelas
-- que a aplicação apenas lê ou que são de infraestrutura/Flyway.
--
-- Motivação: a V034 normalizou o DML das tabelas de negócio, mas crm_app
-- herdou, de grants anteriores, INSERT/UPDATE/DELETE em:
--   * permissions            -- catalogo de referência (só leitura; seeds sao migração)
--   * flyway_schema_history  -- gerenciada pelo Flyway (crm_admin)
--
-- Aplicação (auditado): PermissionService/RoleService/PermissionRepository
-- apenas leem permissions. flyway_schema_history é tocado exclusivamente pelo
-- Flyway. Portanto revogar escrita mantém SELECT e não quebra o app.
--
-- Idempotente — seguro reaplicar.
REVOKE INSERT, UPDATE, DELETE ON public.permissions FROM crm_app;
REVOKE INSERT, UPDATE, DELETE ON public.flyway_schema_history FROM crm_app;

GRANT SELECT ON public.permissions TO crm_app;
GRANT SELECT ON public.flyway_schema_history TO crm_app;