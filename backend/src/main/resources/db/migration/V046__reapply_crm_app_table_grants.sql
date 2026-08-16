-- V046__reapply_crm_app_table_grants.sql
-- Correção de governança: tabelas criadas APÓS a V034 ficaram sem GRANT para crm_app.
--
-- Contexto: a V034 aplica CRUD à crm_app via loop dinâmico sobre pg_tables public,
-- mas esse loop só roda uma vez (no momento da V034). As tabelas criadas depois
-- (V037, V039, V041, V043, V044) nunca passaram pelo loop e, por isso, operações
-- de leitura/escrita retornavam HTTP 500 com `permission denied for table ...`.
--
-- Este script reexecuta o mesmo loop idempotente da V034, agora pegando também:
--   * activities, tasks (V039)
--   * workflows, workflow_conditions, workflow_actions, workflow_executions (V041)
--   * workflow_runs (V043)
--   * omnichannel_channels, omnichannel_conversations, omnichannel_messages (V044)
--
-- Convenção (idêntica à V034):
--   * Toda tabela de aplicação recebe GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app.
--   * Não usar GRANT ALL.
--   * Exceções fora do DML:
--       - flyway_schema_history: gerenciada pelo Flyway (crm_admin), não tocar.
--       - permissions: tabela de referência (seed); a aplicação apenas lê (SELECT).
--   * GRANT não afeta RLS: crm_app é NOBYPASSRLS, o isolamento por tenant continua.
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

-- ===========================================================================
-- Verificação estrutural (pg_catalog, imune a RLS)
-- ===========================================================================
DO $$
DECLARE
    missing text[];
BEGIN
    SELECT COALESCE(array_agg(t), '{}')
      INTO missing
      FROM (
          SELECT tablename AS t
          FROM pg_tables
          WHERE schemaname = 'public'
            AND tablename NOT IN ('flyway_schema_history', 'permissions')
          EXCEPT
          SELECT table_name
          FROM information_schema.role_table_grants
          WHERE grantee = 'crm_app'
            AND table_schema = 'public'
            AND privilege_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE')
      ) sub;

    IF array_length(missing, 1) > 0 THEN
        RAISE EXCEPTION 'V046: tabelas sem CRUD para crm_app: %', missing;
    END IF;

    RAISE NOTICE 'V046: grants CRUD reaplicados para crm_app em todas as tabelas public';
END $$;