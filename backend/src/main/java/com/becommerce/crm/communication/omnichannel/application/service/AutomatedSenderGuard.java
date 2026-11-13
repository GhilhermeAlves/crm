package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelInboxNotifier;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ações do Leo quando o outro lado não é um paciente:
 * <ul>
 *   <li>{@link #ignore}: robô com certeza (sinal forte do
 *       {@link AutomatedSenderDetector}) — o número vai para a lista de ignorados
 *       e o webhook passa a descartar as mensagens dele;</li>
 *   <li>{@link #handToTeam}: suspeita (do agente ou indício fraco) — a IA só
 *       pausa nesta conversa e a equipe é avisada. Nada é ignorado: pessoas
 *       (amigos, família, fornecedores) eram marcadas como robô e sumiam.</li>
 * </ul>
 * Chamado com o {@link TenantContext} da empresa já definido.
 */
@Component
public class AutomatedSenderGuard {

    private static final Logger log = LoggerFactory.getLogger(AutomatedSenderGuard.class);

    static final String LABEL = "Robô / mensagem automática (Leo)";

    static final long DEFAULT_HANDOFF_PAUSE_HOURS = 168;

    private final OmnichannelIgnoredContactRepository ignoredContactRepository;
    private final OmnichannelInboxNotifier inboxNotifier;
    private final OmnichannelConversationRepository conversationRepository;
    private final Duration handoffPause;

    @Autowired
    public AutomatedSenderGuard(OmnichannelIgnoredContactRepository ignoredContactRepository,
                                OmnichannelInboxNotifier inboxNotifier,
                                OmnichannelConversationRepository conversationRepository,
                                @Value("${omnichannel.whatsapp.handoff-pause-hours:168}") long handoffPauseHours) {
        this.ignoredContactRepository = ignoredContactRepository;
        this.inboxNotifier = inboxNotifier;
        this.conversationRepository = conversationRepository;
        this.handoffPause = Duration.ofHours(Math.max(1, handoffPauseHours));
    }

    /**
     * Deixa a conversa com a equipe: a IA não responde nela até alguém devolver
     * ao automático ou o prazo vencer (padrão 7 dias). O número NÃO é ignorado.
     *
     * @return {@code true} se a conversa foi pausada
     */
    public boolean handToTeam(UUID companyId, UUID conversationId, String phone, String reason) {
        boolean paused = conversationRepository.findById(conversationId)
                .filter(c -> companyId.equals(c.getCompanyId()))
                .map(c -> {
                    c.pauseAutomationUntil(LocalDateTime.now().plus(handoffPause));
                    conversationRepository.save(c);
                    return true;
                })
                .orElse(false);
        if (!paused) {
            log.warn("[WHATSAPP][HANDOFF] conversa não encontrada (company={}, conversation={})",
                    companyId, conversationId);
            return false;
        }
        try {
            inboxNotifier.notifyHandedToTeam(companyId, conversationId, IgnoredContact.normalizePhone(phone),
                    reason, handoffPause.toDays());
        } catch (RuntimeException e) {
            log.warn("[WHATSAPP][HANDOFF] falha ao notificar a equipe (company={}): {}",
                    companyId, e.getMessage());
        } finally {
            // A notificação roda em transação própria e pode limpar o tenant.
            TenantContext.setCompanyId(companyId);
        }
        log.info("[WHATSAPP][HANDOFF] conversa deixada com a equipe (company={}, conversation={}, motivo={})",
                companyId, conversationId, reason);
        return true;
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
