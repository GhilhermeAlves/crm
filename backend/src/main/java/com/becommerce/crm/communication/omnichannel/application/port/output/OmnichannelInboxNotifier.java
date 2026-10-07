package com.becommerce.crm.communication.omnichannel.application.port.output;

import java.util.UUID;

/**
 * Avisa a equipe da empresa (central de notificações) que uma conversa do
 * WhatsApp recebeu mensagem nova. Falhas não devem interromper o webhook.
 */
public interface OmnichannelInboxNotifier {

    void notifyNewInbound(UUID companyId, UUID conversationId, String fromPhone, String body);
}
