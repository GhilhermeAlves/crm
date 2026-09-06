-- V073__followup_tables.sql
-- Sprint 22 (FollowUp): follow-ups de conversa omnichannel + suporte ao
-- processador multi-tenant agendado.
--
-- Padrão V044/V039: company_id FK para companies + RLS ENABLE/FORCE + policy
-- tenant_isolation_policy com app.current_tenant_id(). Grants CRUD para o role
-- crm_app reaplicados pelo loop dinâmico (padrão V062). FKs compostas com
-- escopo de tenant (defesa em profundidade, padrão V054).

-- ===========================================================================
-- followups — ação futura associada a uma conversa (AUTOMATIC/HUMAN).
-- Estado: PENDING -> PROCESSING -> SENT | FAILED | CANCELLED
--         PROCESSING -> PENDING (retry seguro) | CANCELLED (regras do domínio:
--                        HUMAN mode, obsoleto por nova mensagem do cliente).
-- Idempotência de execução: claim atômico UPDATE ... WHERE status='PENDING'
--   (apenas UMA instância do worker vence por follow-up, ver app.followup...
--   + repositório). Idempotência de criação: idempotency_key UUID opcional
--   (única por empresa) — o serviço retorna o FollowUp existente.
-- ===========================================================================
CREATE TABLE followups (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    conversation_id      UUID NOT NULL REFERENCES omnichannel_conversations(id) ON DELETE CASCADE,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    action_type          VARCHAR(20) NOT NULL DEFAULT 'SEND_MESSAGE',
    action_content       TEXT,
    execute_at           TIMESTAMP NOT NULL,
    attempts             INT NOT NULL DEFAULT 0,
    last_error           TEXT,
    result_text          TEXT,
    processing_started_at TIMESTAMP,
    processed_at         TIMESTAMP,
    cancelled_at         TIMESTAMP,
    cancelled_reason     VARCHAR(40),
    idempotency_key      UUID,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_followups_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_followups_action
        CHECK (action_type IN ('SEND_MESSAGE')),
    CONSTRAINT chk_followups_cancelled_reason
        CHECK (cancelled_reason IS NULL OR cancelled_reason IN ('USER', 'HUMAN_MODE', 'SUPERSEDED_BY_NEW_MESSAGE'))
);

-- Tenant-compound FK: conversa só pode ser da MESMA empresa (V054).
ALTER TABLE followups
    ADD CONSTRAINT fk_followups_conversation_tenant
    FOREIGN KEY (conversation_id, company_id)
    REFERENCES omnichannel_conversations (id, company_id) ON DELETE CASCADE;

CREATE INDEX idx_followups_company ON followups (company_id);
CREATE INDEX idx_followups_conversation_created ON followups (conversation_id, created_at);
-- Varredura do processador: apenas PENDING vencidos (retry seguro usa status PENDING).
CREATE INDEX idx_followups_due ON followups (execute_at) WHERE status = 'PENDING';
CREATE UNIQUE INDEX uq_followups_company_idempotency
    ON followups (company_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE followups ENABLE ROW LEVEL SECURITY;
ALTER TABLE followups FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON followups
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- app.followup_scheduler_candidates(limit) — SECURITY DEFINER (padrão V062).
-- Permite ao worker agendado (thread SEM contexto de tenant) enumerar em TODAS
-- as empresas: PENDING vencidos e PROCESSING órfãos (claim antigo > 15min,
-- recuperação de crash). O restante do processamento continua sob RLS FORCE
-- via GUC + claim atômico.
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.followup_scheduler_candidates(p_limit INT)
RETURNS TABLE(followup_id UUID, company_id UUID, status VARCHAR)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT f.id, f.company_id, f.status
      FROM followups f
     WHERE (f.status = 'PENDING' AND f.execute_at <= NOW())
        OR (f.status = 'PROCESSING' AND f.processing_started_at < NOW() - INTERVAL '15 minutes')
     ORDER BY f.execute_at
     LIMIT p_limit;
$$;

REVOKE ALL ON FUNCTION app.followup_scheduler_candidates(INT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.followup_scheduler_candidates(INT) TO crm_app;

-- ===========================================================================
-- Grants CRUD da tabela nova para crm_app (loop dinâmico, padrão V062);
-- GRANT não afeta RLS (crm_app é NOBYPASSRLS).
-- ===========================================================================
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