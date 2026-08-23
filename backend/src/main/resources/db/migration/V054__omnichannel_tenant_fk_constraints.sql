-- V054__omnichannel_tenant_fk_constraints.sql
-- Sprint 16 hardening (defesa em profundidade): garante no BANCO que uma
-- mensagem só possa referenciar conversa e canal da MESMA empresa.
-- Antes disso, INSERT direto com company_id do próprio tenant apontando para
-- conversa/canal de outro tenant passava pelo RLS (WITH CHECK olha apenas
-- company_id da linha) e pelas FKs simples (que não checam tenant).
--
-- Estratégia: FKs compostas (entidade_id, company_id) -> unique (id, company_id).
-- Idempotente: verifica existence antes de cada ALTER (IF NOT EXISTS via DO).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_omnichannel_conversations_id_company') THEN
        ALTER TABLE omnichannel_conversations
            ADD CONSTRAINT uq_omnichannel_conversations_id_company UNIQUE (id, company_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_omnichannel_channels_id_company') THEN
        ALTER TABLE omnichannel_channels
            ADD CONSTRAINT uq_omnichannel_channels_id_company UNIQUE (id, company_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_omnichannel_messages_conversation_tenant') THEN
        ALTER TABLE omnichannel_messages
            ADD CONSTRAINT fk_omnichannel_messages_conversation_tenant
            FOREIGN KEY (conversation_id, company_id)
            REFERENCES omnichannel_conversations (id, company_id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_omnichannel_messages_channel_tenant') THEN
        ALTER TABLE omnichannel_messages
            ADD CONSTRAINT fk_omnichannel_messages_channel_tenant
            FOREIGN KEY (channel_id, company_id)
            REFERENCES omnichannel_channels (id, company_id) ON DELETE CASCADE;
    END IF;
END $$;

-- Revalida dados existentes (falha se houver inconsistência histórica).
ALTER TABLE omnichannel_messages VALIDATE CONSTRAINT fk_omnichannel_messages_conversation_tenant;
ALTER TABLE omnichannel_messages VALIDATE CONSTRAINT fk_omnichannel_messages_channel_tenant;
