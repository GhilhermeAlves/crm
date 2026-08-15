package com.becommerce.crm.application.omnichannel.port.output;

import java.util.UUID;

/**
 * Abstração de provider de mensageria, desacoplada da API externa.
 * O domínio de CRM não conhece classes da Meta; o adapter concreto
 * (WhatsApp Cloud API, fake, etc.) implementa esta porta.
 */
public interface WhatsAppProvider {

    /** Resultado de envio: identificador externo da mensagem (wamid). */
    record SendResult(String externalMessageId) {
    }

    record SendRequest(UUID companyId, UUID channelId, String phoneNumberId,
                       String to, String body) {
    }

    /**
     * Envia uma mensagem de texto. Retorna o identificador externo do provedor.
     * Lança {@link com.becommerce.crm.domain.omnichannel.OmnichannelProviderException} em falha.
     */
    SendResult send(SendRequest request);

    /** Nome do provider (para logs/observabilidade, sem expor secrets). */
    String providerName();
}