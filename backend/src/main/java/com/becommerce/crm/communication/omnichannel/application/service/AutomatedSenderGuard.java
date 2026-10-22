package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelInboxNotifier;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ação do Leo quando o outro lado é um robô: coloca o número na lista de
 * ignorados (o webhook passa a descartar as mensagens dele, sem resposta da IA)
 * e avisa a equipe, que pode remover o número de lá se for uma pessoa.
 * Chamado com o {@link TenantContext} da empresa já definido.
 */
@Component
public class AutomatedSenderGuard {

    private static final Logger log = LoggerFactory.getLogger(AutomatedSenderGuard.class);

    static final String LABEL = "Robô / mensagem automática (Leo)";

    private final OmnichannelIgnoredContactRepository ignoredContactRepository;
    private final OmnichannelInboxNotifier inboxNotifier;

    public AutomatedSenderGuard(OmnichannelIgnoredContactRepository ignoredContactRepository,
                                OmnichannelInboxNotifier inboxNotifier) {
        this.ignoredContactRepository = ignoredContactRepository;
        this.inboxNotifier = inboxNotifier;
    }

    /** @return {@code true} se o número foi (ou já estava) colocado na lista de ignorados. */
    public boolean ignore(UUID companyId, UUID conversationId, String phone, String reason) {
        String normalized = IgnoredContact.normalizePhone(phone);
        if (normalized.length() < 10) {
            log.warn("[WHATSAPP][BOT] telefone inválido para ignorar (company={}, conversation={})",
                    companyId, conversationId);
            return false;
        }
        if (!ignoredContactRepository.existsByCompanyAndPhone(companyId, normalized)) {
            ignoredContactRepository.save(new IgnoredContact(UUID.randomUUID(), companyId, normalized, LABEL,
                    LocalDateTime.now()));
            try {
                inboxNotifier.notifyAutomatedSenderIgnored(companyId, conversationId, normalized, reason);
            } catch (RuntimeException e) {
                log.warn("[WHATSAPP][BOT] falha ao notificar robô ignorado (company={}): {}",
                        companyId, e.getMessage());
            } finally {
                // A notificação roda em transação própria e pode limpar o tenant.
                TenantContext.setCompanyId(companyId);
            }
        }
        log.info("[WHATSAPP][BOT] número ignorado como robô (company={}, conversation={}, motivo={})",
                companyId, conversationId, reason);
        return true;
    }
}
