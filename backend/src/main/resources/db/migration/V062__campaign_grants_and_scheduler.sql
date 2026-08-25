-- V062__campaign_grants_and_scheduler.sql
-- Sprint 17: governança das tabelas novas + suporte ao scheduler multi-tenant.
--
-- 1) Mesmo padrão da V046: tabelas criadas após a V034 não recebem GRANT no
--    momento da criação. Reaplica o loop idempotente de grants CRUD para
--    crm_app (GRANT não afeta RLS — crm_app continua NOBYPASSRLS).
--
-- 2) app.campaign_scheduler_candidates(limit) — SECURITY DEFINER, mesmo
--    padrão da app.resolve_channel_company (V044): permite ao scheduler
--    (thread SEM contexto de tenant) listar campanhas agendadas vencidas em
--    TODAS as empresas sem quebrar o isolamento — o restante da execução
--    continua sob RLS FORCE via GUC + claim atômico por empresa.
--
-- Idempotente - seguro reaplicar.

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

CREATE OR REPLACE FUNCTION app.campaign_scheduler_candidates(p_limit INT)
RETURNS TABLE(campaign_id UUID, company_id UUID)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT c.id, c.company_id
      FROM campaigns c
     WHERE c.status = 'SCHEDULED'
       AND c.scheduled_at IS NOT NULL
       AND c.scheduled_at <= NOW()
     ORDER BY c.scheduled_at
     LIMIT p_limit;
$$;

REVOKE ALL ON FUNCTION app.campaign_scheduler_candidates(INT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.campaign_scheduler_candidates(INT) TO crm_app;
