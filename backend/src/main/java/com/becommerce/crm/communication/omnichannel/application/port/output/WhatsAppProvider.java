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

    /** Arquivo de uma mensagem recebida (áudio, imagem, documento). */
    record MediaContent(byte[] data, String mimeType, String fileName) {
    }

    /**
     * Baixa a mídia de uma mensagem recebida. {@code instance} é o externalId do
     * canal. Vazio quando o provider não suporta ou a mídia não está disponível.
     */
    default java.util.Optional<MediaContent> downloadMedia(String instance, String externalMessageId,
                                                           String secretsRef) {
        return java.util.Optional.empty();
    }

    /**
     * Envia uma mensagem de voz (nota de voz do WhatsApp). Providers sem suporte
     * lançam {@link com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException}
     * — o chamador cai para texto.
     */
    default SendResult sendVoice(SendRequest request, byte[] audio) {
        throw new com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException(
                providerName() + " não envia voz");
    }
}