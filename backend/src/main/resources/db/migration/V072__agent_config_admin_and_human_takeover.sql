-- V072__agent_config_admin_and_human_takeover.sql
-- Sprint 3 (AgentConfig Administrativo + Human Takeover).
--
-- A) Duas novas permissoes no catalogo:
--      * ai:agent-config          -> admin configura o agente de IA (GET/PUT
--                                   /api/v1/ai/agent-config). Concedida a
--                                   SUPER_ADMIN e ADMIN.
--      * omnichannel:takeover     -> humano assume uma conversa / restabelece a
--                                   IA autônoma (POST /inbox/{id}/takeover e
--                                   /release). Concedida a SUPER_ADMIN, ADMIN,
--                                   MANAGER e AGENT (VIEWER permanece
--                                   somente-leitura).
--    Mesmo padrao da V049/V052/V053: INSERT ... ON CONFLICT (name) DO NOTHING
--    + vínculo role->permission por empresa via DO block com
--    app.current_company_id (RLS exige tenant no INSERT de role_permissions).
--    SUPER_ADMIN entra explicitamente (o wildcard "*" do RoleSeedService foi
--    no-op em empresas existentes — ver V069).
--
-- B) Modo da conversa: coluna handoff_mode em omnichannel_conversations.
--    State persistido na conversa (NAO gambiarra via systemPrompt vazio ou
--    allow_auto_reply=false global): quando HUMAN, o
--    WhatsAppInboundAutoReplyProcessor nao reserva/gera/envia auto-resposta.
--    DEFAULT 'AUTOMATIC' preserva o comportamento atual de todas as conversas.

-- ===========================================================================
-- A) Permissoes novas
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('ai:agent-config', 'Manage the AI agent configuration of the company', 'ai', 'agent-config', 'manage'),
    ('omnichannel:takeover', 'Take over and release conversations from the AI agent', 'omnichannel', 'conversation', 'takeover')
ON CONFLICT (name) DO NOTHING;

-- Grants para TODAS as empresas existentes (matrix de least privilege).
-- Empresas novas sao cobertas no startup pelo RoleSeedService (V052/V053).
DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE p.name IN ('ai:agent-config', 'omnichannel:takeover')
          AND (
              (r.name IN ('SUPER_ADMIN', 'ADMIN'))
              OR (r.name IN ('MANAGER', 'AGENT') AND p.name = 'omnichannel:takeover')
          )
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V072: granted % takeover/agent-config permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;

-- ===========================================================================
-- B) handoff_mode na conversa
-- ===========================================================================
ALTER TABLE omnichannel_conversations
    ADD COLUMN handoff_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC';

ALTER TABLE omnichannel_conversations
    ADD CONSTRAINT chk_omnichannel_conversations_handoff_mode
        CHECK (handoff_mode IN ('AUTOMATIC', 'HUMAN'));