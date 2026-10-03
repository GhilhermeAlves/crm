package com.becommerce.crm.communication.omnichannel.domain;

/** Provedor de canal. */
public enum ChannelProvider {
    WHATSAPP_CLOUD_API,
    UAZAPI,
    /** Provedor fake para desenvolvimento/testes (nenhum tráfego real). */
    FAKE
}
