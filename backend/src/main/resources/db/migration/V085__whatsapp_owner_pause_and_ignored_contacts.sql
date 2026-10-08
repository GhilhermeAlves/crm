-- V085__whatsapp_owner_pause_and_ignored_contacts.sql
-- Número do WhatsApp compartilhado com o celular do profissional:
-- A) Quando o dono do número responde pelo celular, a conversa vai para modo
--    HUMAN até human_until (a IA volta sozinha depois). NULL = sem prazo
--    (takeover manual pela Inbox).
-- B) Contatos ignorados por empresa: mensagens desses números não são gravadas
--    nem respondidas (família/amigos no número pessoal).

ALTER TABLE omnichannel_conversations
    ADD COLUMN human_until TIMESTAMP;

CREATE TABLE omnichannel_ignored_contacts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    phone       VARCHAR(40) NOT NULL,          -- só dígitos, com DDI (ex.: 5534999998888)
    label       VARCHAR(120),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_omnichannel_ignored_contacts_company_phone
    ON omnichannel_ignored_contacts (company_id, phone);

ALTER TABLE omnichannel_ignored_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE omnichannel_ignored_contacts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON omnichannel_ignored_contacts
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON omnichannel_ignored_contacts TO crm_app;
