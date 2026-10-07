package com.becommerce.crm.communication.omnichannel.application.port.output;

import java.util.UUID;

/**
 * Abstração de provider de mensageria, desacoplada da API externa.
 * O domínio de CRM não conhece classes do provider; o adapter concreto
 * (Evolution API, fake) implementa esta porta.
 */
public interface WhatsAppProvider {

    /** Resultado de envio: identificador externo da mensagem (key.id). */
    record SendResult(String externalMessageId) {
    }

    record SendRequest(UUID companyId, UUID channelId, String phoneNumberId,
                       String to, String body, String secretsRef) {
    }

    /**
     * Envia uma mensagem de texto. Retorna o identificador externo do provedor.
     * Lança {@link com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException} em falha.
     */
    SendResult send(SendRequest request);

    /** Nome do provider (para logs/observabilidade, sem expor secrets). */
    String providerName();
}