package com.becommerce.crm.communication.omnichannel.infrastructure;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelInboxNotifier;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cria uma notificação {@link NotificationType#MESSAGE} para cada usuário ativo
 * da empresa. Roda em transação própria: uma falha aqui não desfaz a mensagem
 * já gravada pelo webhook.
 */
@Component
public class OmnichannelInboxNotifierImpl implements OmnichannelInboxNotifier {

    private static final Logger log = LoggerFactory.getLogger(OmnichannelInboxNotifierImpl.class);
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);
    private static final int PREVIEW_MAX = 120;

    private final UserRepository userRepository;
    private final NotificationUseCase notificationUseCase;

    public OmnichannelInboxNotifierImpl(UserRepository userRepository, NotificationUseCase notificationUseCase) {
        this.userRepository = userRepository;
        this.notificationUseCase = notificationUseCase;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyNewInbound(UUID companyId, UUID conversationId, String fromPhone, String body) {
        String title = "Nova mensagem no WhatsApp";
        String text = formatPhone(fromPhone) + ": " + preview(body);
        String metadata = "{\"conversationId\":\"" + conversationId + "\"}";
        for (User user : userRepository.findAllByCompanyId(companyId)) {
            if (user.getStatus() == null || !user.getStatus().isActive()) {
                continue;
            }
            notificationUseCase.create(companyId,
                    new CreateNotificationRequest(user.getId(), NotificationType.MESSAGE, title, text, metadata),
                    SYSTEM_ACTOR);
        }
        log.info("[WHATSAPP][NOTIFY] companyId={} conversationId={} notificação de mensagem criada",
                companyId, conversationId);
    }

    private static String preview(String body) {
        if (body == null || body.isBlank()) {
            return "(sem texto)";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= PREVIEW_MAX ? oneLine : oneLine.substring(0, PREVIEW_MAX - 1) + "…";
    }

    /** 5534991546422 → +55 (34) 99154-6422; outros formatos ficam como vieram. */
    static String formatPhone(String digits) {
        if (digits != null && digits.matches("55\\d{10,11}")) {
            String ddd = digits.substring(2, 4);
            String local = digits.substring(4);
            int split = local.length() - 4;
            return "+55 (" + ddd + ") " + local.substring(0, split) + "-" + local.substring(split);
        }
        return digits == null ? "Contato" : "+" + digits;
    }
}
