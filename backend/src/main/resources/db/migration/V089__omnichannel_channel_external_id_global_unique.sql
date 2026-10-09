-- ===========================================================================
-- external_id do canal (instância Evolution / phone number id) passa a ser
-- único no sistema todo, não só por empresa: o webhook resolve a empresa pelo
-- external_id (app.resolve_channel_company, LIMIT 1), então um nome repetido
-- entre empresas roteava mensagens para a empresa errada.
-- Falha de propósito se já houver duplicados — resolver manualmente antes.
-- ===========================================================================
DROP INDEX IF EXISTS uq_omnichannel_channels_company_external;

CREATE UNIQUE INDEX uq_omnichannel_channels_external
    ON omnichannel_channels (external_id) WHERE external_id IS NOT NULL;
