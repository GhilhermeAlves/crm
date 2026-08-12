-- V034__standardize_crm_app_table_grants.sql
-- Governança (Parte 1.2): padroniza os grants do usuário de aplicação (crm_app).
--
-- Contexto: algumas tabelas (companies, permissions, role_permissions) estavam
-- com apenas SELECT para crm_app, causando HTTP 500 em operações de escrita.
-- Este padrão segue a V031 (GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app).
--
-- Convenção para migrations novas (documentada em sprints/8.5/REPORT.md):
--   * Toda tabela de aplicação recebe GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app.
--   * Não usar GRANT ALL (evita privilégios desnecessários).
--   * Exceções mantidas fora do DML (somente leitura / infraestrutura):
--       - flyway_schema_history: gerenciada pelo Flyway (crm_admin), não tocar.
--       - permissions: tabela de referência (seed); a aplicação apenas lê.
--   * GRANT não afeta RLS: crm_app é NOBYPASSRLS, o isolamento continua.
--
-- Idempotente — seguro reaplicar.
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT IN ('flyway_schema_history', 'permissions')
        ORDER BY tablename
    LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO crm_app', t);
    END LOOP;
END $$;