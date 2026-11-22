package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.communication.omnichannel.domain.ChannelType;

import java.util.UUID;

/**
 * Avisa a equipe da empresa (central de notificações) que uma conversa do
 * WhatsApp/Instagram recebeu mensagem nova. Falhas não devem interromper o webhook.
 */
public interface OmnichannelInboxNotifier {

    void notifyNewInbound(UUID companyId, UUID conversationId, ChannelType channelType, String fromPhone, String body);

    /** O Leo identificou um robô/resposta automática e colocou o número na lista de ignorados. */
    void notifyAutomatedSenderIgnored(UUID companyId, UUID conversationId, String phone, String reason);

    /**
     * O Leo deixou a conversa com a equipe (não parece paciente ou pode ser um
     * robô) e parou de responder nela por {@code pauseDays} dias.
     */
    void notifyHandedToTeam(UUID companyId, UUID conversationId, String phone, String reason, long pauseDays);
}
