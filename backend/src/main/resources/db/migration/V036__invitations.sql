-- V036__invitations.sql
-- Sprint 8.5 - Invitations
--
-- Convites de empresa rastreáveis e seguros por e-mail.
--
-- Design (decisões aprovadas):
--   * Status: PENDING | ACCEPTED | REVOKED | EXPIRED. Recusa é mapeada para
--     REVOKED (o modelo de memberships não possui DECLINED; documentado no
--     REPORT).
--   * O token NUNCA é persistido em texto puro: guardamos apenas token_hash
--     (SHA-256 hex, 64 chars).
--   * Acesso por token é separado do acesso administrativo, via GUC dedicado
--     (app.invitation_token_hash) + function app.current_invitation_token_hash(),
--     seguindo o mesmo mecanismo do tenant (app.current_tenant_id, V019/V030).
--   * RLS FORCE por company_id para o acesso administrativo; policies extras
--     por token_hash liberam apenas a linha do convite para aceite/recusa.
--   * Roles fixas no convite: ADMIN | MANAGER | AGENT | VIEWER. SUPER_ADMIN
--     jamais concedível; OWNER permanece exclusivo do onboarding.
--
-- ===========================================================================
-- 1. Funções de contexto de token (p/ policies RLS de acesso por token)
-- ===========================================================================
CREATE OR REPLACE FUNCTION app.set_invitation_token_context(p_hash text)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    -- local à transação corrente
    PERFORM set_config('app.invitation_token_hash', p_hash, true);
END $$;

CREATE OR REPLACE FUNCTION app.current_invitation_token_hash()
RETURNS text
LANGUAGE sql STABLE
AS $$
    SELECT nullif(current_setting('app.invitation_token_hash', true), '');
$$;

GRANT EXECUTE ON FUNCTION app.set_invitation_token_context(text) TO crm_app;
GRANT EXECUTE ON FUNCTION app.current_invitation_token_hash() TO crm_app;

-- ===========================================================================
-- 2. Tabela
-- ===========================================================================
CREATE TABLE invitations (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    company_id  uuid         NOT NULL,
    email       varchar(255) NOT NULL,
    role        varchar(50)  NOT NULL,
    token_hash  varchar(64)  NOT NULL,
    invited_by  uuid,
    status      varchar(20)  NOT NULL DEFAULT 'PENDING',
    expires_at  timestamp    NOT NULL,
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT invitations_pkey PRIMARY KEY (id),
    CONSTRAINT fk_invitations_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT invitations_status_check CHECK (status IN ('PENDING','ACCEPTED','REVOKED','EXPIRED'))
);

-- ---------------------------------------------------------------------------
-- Índices
-- ---------------------------------------------------------------------------
CREATE INDEX idx_invitations_company_id ON invitations(company_id);
CREATE INDEX idx_invitations_email ON invitations(email);
CREATE INDEX idx_invitations_status ON invitations(status);
CREATE UNIQUE INDEX idx_invitations_token_hash ON invitations(token_hash);
-- Um único convite PENDENTE por (empresa, e-mail)
CREATE UNIQUE INDEX uq_invitations_pending_company_email
    ON invitations(company_id, email)
    WHERE status = 'PENDING';

-- ===========================================================================
-- 3. RLS FORCE
-- ===========================================================================
ALTER TABLE invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitations FORCE ROW LEVEL SECURITY;

-- Acesso administrativo (RBAC garante ADMIN/OWNER no serviço): apenas convites
-- da própria empresa.
CREATE POLICY invitations_admin_select_policy ON invitations
    FOR SELECT USING (company_id = app.current_tenant_id());

CREATE POLICY invitations_admin_write_policy ON invitations
    FOR ALL USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- Acesso por token (aceite/recusa): quem apresenta o token correto lê/atualiza
-- aquele convite, ainda que não seja membro da empresa ainda. Separação
-- explícita do acesso administrativo (cross-tenant não vaza).
CREATE POLICY invitations_token_select_policy ON invitations
    FOR SELECT USING (token_hash = app.current_invitation_token_hash());

CREATE POLICY invitations_token_update_policy ON invitations
    FOR UPDATE USING (token_hash = app.current_invitation_token_hash())
    WITH CHECK (token_hash = app.current_invitation_token_hash());

-- ===========================================================================
-- 4. Grants (padrão V031/V034)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON invitations TO crm_app;