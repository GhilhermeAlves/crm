package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.communication.telegram.domain.TelegramLink;
import com.becommerce.crm.communication.telegram.domain.TelegramLinkCode;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramLinkCodeJpaRepository;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramLinkJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Vínculo entre um usuário do CRM e uma conta do Telegram, por código de uso único. */
@Service
public class TelegramLinkService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final TelegramLinkJpaRepository links;
    private final TelegramLinkCodeJpaRepository codes;
    private final TenantAuditRecorder auditor;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TelegramLinkService(TelegramLinkJpaRepository links, TelegramLinkCodeJpaRepository codes,
                               TenantAuditRecorder auditor, Clock clock) {
        this.links = links;
        this.codes = codes;
        this.auditor = auditor;
        this.clock = clock;
    }

    public enum LinkResult { LINKED, INVALID_CODE, TELEGRAM_ALREADY_LINKED }

    public record LinkView(UUID userId, String telegramName, LocalDateTime createdAt) {}

    @Transactional
    public String generateCode(UUID companyId, UUID userId) {
        return TelegramConfigService.withTenant(companyId, () -> {
            codes.deleteByCompanyIdAndUserId(companyId, userId);
            String code = String.format("%06d", random.nextInt(1_000_000));
            TelegramLinkCode c = new TelegramLinkCode();
            c.setCodeHash(hash(companyId, code));
            c.setCompanyId(companyId);
            c.setUserId(userId);
            c.setExpiresAt(now().plus(TTL));
            codes.save(c);
            return code;
        });
    }

    @Transactional
    public LinkResult link(UUID companyId, String code, long telegramUserId, long chatId, String telegramName) {
        return TelegramConfigService.withTenant(companyId, () -> {
            Optional<TelegramLinkCode> found = code == null
                    ? Optional.empty() : codes.findById(hash(companyId, code.strip()));
            if (found.isEmpty() || found.get().getUsedAt() != null || found.get().getExpiresAt().isBefore(now())) {
                return LinkResult.INVALID_CODE;
            }
            TelegramLinkCode c = found.get();
            Optional<TelegramLink> byTelegram = links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId);
            if (byTelegram.isPresent() && !byTelegram.get().getUserId().equals(c.getUserId())) {
                return LinkResult.TELEGRAM_ALREADY_LINKED;
            }
            c.setUsedAt(now());
            TelegramLink link = links.findByCompanyIdAndUserId(companyId, c.getUserId()).orElseGet(TelegramLink::new);
            link.setCompanyId(companyId);
            link.setUserId(c.getUserId());
            link.setTelegramUserId(telegramUserId);
            link.setChatId(chatId);
            link.setTelegramName(telegramName);
            links.save(link);
            audit(companyId, c.getUserId(), "TELEGRAM_LINKED", "Telegram vinculado ao usuário");
            return LinkResult.LINKED;
        });
    }

    @Transactional
    public boolean unlinkByTelegram(UUID companyId, long telegramUserId) {
        return TelegramConfigService.withTenant(companyId,
                () -> links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId)
                        .map(l -> {
                            links.delete(l);
                            audit(companyId, l.getUserId(), "TELEGRAM_UNLINKED", "Telegram desvinculado");
                            return true;
                        })
                        .orElse(false));
    }

    @Transactional
    public void unlinkUser(UUID companyId, UUID userId) {
        TelegramConfigService.withTenant(companyId, () -> {
            links.findByCompanyIdAndUserId(companyId, userId).ifPresent(l -> {
                links.delete(l);
                audit(companyId, userId, "TELEGRAM_UNLINKED", "Telegram desvinculado");
            });
            return null;
        });
    }

    @Transactional(readOnly = true)
    public Optional<LinkView> myLink(UUID companyId, UUID userId) {
        return TelegramConfigService.withTenant(companyId,
                () -> links.findByCompanyIdAndUserId(companyId, userId).map(this::view));
    }

    @Transactional(readOnly = true)
    public List<LinkView> listLinks(UUID companyId) {
        return TelegramConfigService.withTenant(companyId,
                () -> links.findByCompanyIdOrderByCreatedAtDesc(companyId).stream().map(this::view).toList());
    }

    @Transactional
    public void setConversation(UUID companyId, long telegramUserId, UUID conversationId) {
        TelegramConfigService.withTenant(companyId, () -> {
            links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId).ifPresent(l -> {
                l.setAiConversationId(conversationId);
                links.save(l);
            });
            return null;
        });
    }

    private LinkView view(TelegramLink l) {
        return new LinkView(l.getUserId(), l.getTelegramName(), l.getCreatedAt());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    private static String hash(UUID companyId, String code) {
        return TelegramConfigService.sha256(companyId + ":" + code);
    }

    private void audit(UUID companyId, UUID userId, String event, String description) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "TelegramLink", userId.toString(),
                    description, userId, Map.of("event", event, "channel", "telegram"));
        } catch (RuntimeException ignored) {
            // auditoria nunca derruba o fluxo
        }
    }
}
