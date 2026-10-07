package com.becommerce.crm.communication.omnichannel.domain;

/** Provedor de canal. */
public enum ChannelProvider {
    /** Evolution API v2 self-hosted (instância = externalId do canal). */
    EVOLUTION,
    /** Provedor fake para desenvolvimento/testes (nenhum tráfego real). */
    FAKE
}
