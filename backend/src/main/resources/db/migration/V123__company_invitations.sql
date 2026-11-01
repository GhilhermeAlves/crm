-- V123__company_invitations.sql
-- Convite de criação de empresa: só o CRM_ADMIN envia. Ele define especialidade
-- (segmento), módulos, plano e limite de usuários; o convidado cria a conta e
-- preenche os dados da empresa. No aceite a empresa nasce com o convidado como
-- dono (membership OWNER, papel RBAC ADMIN); o CRM_ADMIN não vira membro.
--
-- Como a empresa ainda não existe, o convite pertence à empresa de quem o
-- enviou (inviter_company_id) para o acesso administrativo sob RLS. O acesso
-- público usa o mesmo GUC de token dos convites de funcionário (V036).
-- O token nunca é guardado em claro: só token_hash (SHA-256 hex).

CREATE TABLE company_invitations (
    id                 uuid         NOT NULL DEFAULT gen_random_uuid(),
    inviter_company_id uuid         NOT NULL,
    email              varchar(255) NOT NULL,
    invitee_name       varchar(255),
    segment            varchar(40)  NOT NULL,
    enabled_modules    text         NOT NULL,
    plan               varchar(20)  NOT NULL,
    max_users          integer      NOT NULL,
    token_hash         varchar(64)  NOT NULL,
    invited_by         uuid,
    status             varchar(20)  NOT NULL DEFAULT 'PENDING',
    company_id         uuid,
    expires_at         timestamp    NOT NULL,
    created_at         timestamp    NOT NULL DEFAULT now(),
    updated_at         timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT company_invitations_pkey PRIMARY KEY (id),
    CONSTRAINT fk_company_invitations_inviter_company FOREIGN KEY (inviter_company_id)
        REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_invitations_invited_by FOREIGN KEY (invited_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_company_invitations_company FOREIGN KEY (company_id)
        REFERENCES companies(id) ON DELETE SET NULL,
    CONSTRAINT company_invitations_status_check
        CHECK (status IN ('PENDING','ACCEPTED','REVOKED','EXPIRED')),
    CONSTRAINT company_invitations_max_users_check CHECK (max_users > 0)
);

CREATE UNIQUE INDEX idx_company_invitations_token_hash ON company_invitations(token_hash);
CREATE INDEX idx_company_invitations_inviter_company ON company_invitations(inviter_company_id);
-- Um único convite de empresa PENDENTE por e-mail.
CREATE UNIQUE INDEX uq_company_invitations_pending_email
    ON company_invitations(lower(email))
    WHERE status = 'PENDING';

ALTER TABLE company_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_invitations FORCE ROW LEVEL SECURITY;

-- Acesso administrativo (o serviço exige CRM_ADMIN): convites enviados a
-- partir da empresa ativa de quem administra.
CREATE POLICY company_invitations_admin_policy ON company_invitations
    FOR ALL USING (inviter_company_id = app.current_tenant_id())
    WITH CHECK (inviter_company_id = app.current_tenant_id());

-- Acesso por token (prévia, cadastro e aceite).
CREATE POLICY company_invitations_token_select_policy ON company_invitations
    FOR SELECT USING (token_hash = app.current_invitation_token_hash());

CREATE POLICY company_invitations_token_update_policy ON company_invitations
    FOR UPDATE USING (token_hash = app.current_invitation_token_hash())
    WITH CHECK (token_hash = app.current_invitation_token_hash());

GRANT SELECT, INSERT, UPDATE, DELETE ON company_invitations TO crm_app;
