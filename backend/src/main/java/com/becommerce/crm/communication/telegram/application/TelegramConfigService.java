package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.communication.telegram.domain.TelegramBotConfig;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramBotClient;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramBotConfigJpaRepository;
import com.becommerce.crm.masterdata.company.mail.MailPasswordCipher;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Configuração do bot do Telegram por clínica (token cifrado + webhook com segredo). */
@Service
public class TelegramConfigService {

    private static final Logger log = LoggerFactory.getLogger(TelegramConfigService.class);

    private final TelegramBotClient client;
    private final TelegramBotConfigJpaRepository repo;
    private final MailPasswordCipher cipher;
    private final String webhookBaseUrl;
    private final SecureRandom random = new SecureRandom();

    public TelegramConfigService(TelegramBotClient client, TelegramBotConfigJpaRepository repo,
                                 MailPasswordCipher cipher,
                                 @Value("${app.telegram.webhook-base-url:}") String webhookBaseUrl) {
        this.client = client;
        this.repo = repo;
        this.cipher = cipher;
        this.webhookBaseUrl = webhookBaseUrl == null ? "" : webhookBaseUrl.replaceAll("/+$", "");
    }

    public record TelegramConfigResponse(boolean configured, String botUsername, boolean enabled,
                                         LocalDateTime updatedAt) {
        static TelegramConfigResponse empty() {
            return new TelegramConfigResponse(false, null, false, null);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @Transactional(readOnly = true)
    public TelegramConfigResponse get(UUID companyId) {
        return withTenant(companyId, () -> repo.findById(companyId)
                .map(c -> new TelegramConfigResponse(true, c.getBotUsername(), c.isEnabled(), c.getUpdatedAt()))
                .orElse(TelegramConfigResponse.empty()));
    }

    @Transactional
    public TelegramConfigResponse save(UUID companyId, String token, UUID userId) {
        if (webhookBaseUrl.isBlank()) {
            throw badRequest("TELEGRAM_WEBHOOK_BASE_URL não configurada no servidor.");
        }
        if (token == null || token.isBlank()) {
            throw badRequest("Informe o token do bot.");
        }
        String clean = token.strip();
        String username;
        try {
            username = client.getMe(clean);
        } catch (TelegramBotClient.TelegramApiException e) {
            throw badRequest("Token do bot inválido.");
        }
        String secret = newSecret();
        try {
            client.setWebhook(clean, webhookBaseUrl + "/api/v1/telegram/webhook/" + companyId, secret);
        } catch (TelegramBotClient.TelegramApiException e) {
            throw badRequest("O Telegram recusou o webhook. Confira a URL pública do servidor.");
        }
        return withTenant(companyId, () -> {
            TelegramBotConfig c = repo.findById(companyId).orElseGet(TelegramBotConfig::new);
            c.setCompanyId(companyId);
            c.setBotTokenCipher(cipher.encrypt(clean));
            c.setWebhookSecretHash(sha256(secret));
            c.setBotUsername(username);
            c.setEnabled(true);
            c.setUpdatedBy(userId);
            c.setUpdatedAt(LocalDateTime.now());
            repo.save(c);
            return new TelegramConfigResponse(true, username, true, c.getUpdatedAt());
        });
    }

    @Transactional
    public void delete(UUID companyId) {
        withTenant(companyId, () -> {
            repo.findById(companyId).ifPresent(c -> {
                try {
                    client.deleteWebhook(cipher.decrypt(c.getBotTokenCipher()));
                } catch (RuntimeException e) {
                    log.warn("[TELEGRAM] falha ao remover webhook (company={})", companyId);
                }
                repo.delete(c);
            });
            return null;
        });
    }

    /** Token decifrado só se o segredo do webhook bater e a integração estiver ativa. */
    @Transactional(readOnly = true)
    public Optional<String> tokenIfValidSecret(UUID companyId, String providedSecret) {
        if (providedSecret == null || providedSecret.isBlank()) return Optional.empty();
        return withTenant(companyId, () -> repo.findById(companyId)
                .filter(TelegramBotConfig::isEnabled)
                .filter(c -> MessageDigest.isEqual(
                        c.getWebhookSecretHash().getBytes(StandardCharsets.UTF_8),
                        sha256(providedSecret).getBytes(StandardCharsets.UTF_8)))
                .map(c -> cipher.decrypt(c.getBotTokenCipher())));
    }

    @Transactional(readOnly = true)
    public Optional<String> token(UUID companyId) {
        return withTenant(companyId, () -> repo.findById(companyId)
                .filter(TelegramBotConfig::isEnabled)
                .map(c -> cipher.decrypt(c.getBotTokenCipher())));
    }

    private String newSecret() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Executa com o tenant definido, para as consultas sob RLS FORCE. */
    static <T> T withTenant(UUID companyId, Supplier<T> body) {
        UUID previous = TenantContext.getCompanyId();
        try {
            TenantContext.setCompanyId(companyId);
            return body.get();
        } finally {
            if (previous == null) TenantContext.clear();
            else TenantContext.setCompanyId(previous);
        }
    }
}
