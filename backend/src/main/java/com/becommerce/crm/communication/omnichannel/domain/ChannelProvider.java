package com.becommerce.crm.communication.omnichannel.domain;

/** Provedor de canal. */
public enum ChannelProvider {
    /** Evolution API v2 self-hosted (instância = externalId do canal). */
    EVOLUTION,
    /** WhatsApp Cloud API oficial da Meta (phone_number_id = externalId do canal). */
    META_CLOUD,
    /** Instagram Direct via Instagram API com login do Instagram (graph.instagram.com). */
    META_INSTAGRAM,
    /** Provedor fake para desenvolvimento/testes (nenhum tráfego real). */
    FAKE
}
