package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService.TelegramConfigResponse;
import com.becommerce.crm.communication.telegram.domain.TelegramBotConfig;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramBotClient;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramBotConfigJpaRepository;
import com.becommerce.crm.masterdata.company.mail.MailPasswordCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramConfigServiceTest {

    static final UUID CO = UUID.randomUUID(), USER = UUID.randomUUID();

    @Mock TelegramBotClient client;
    @Mock TelegramBotConfigJpaRepository repo;
    MailPasswordCipher cipher = new MailPasswordCipher("k", "");
    TelegramConfigService service;

    @BeforeEach
    void setUp() {
        service = new TelegramConfigService(client, repo, cipher, "https://api.crm.test");
    }

    @Test
    void saveValidatesTokenRegistersWebhookAndStoresCipher() {
        when(client.getMe("TOK")).thenReturn("leo_bot");
        ArgumentCaptor<String> secret = ArgumentCaptor.forClass(String.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        TelegramConfigResponse r = service.save(CO, "TOK", USER);

        verify(client).setWebhook(eq("TOK"), eq("https://api.crm.test/api/v1/telegram/webhook/" + CO), secret.capture());
        ArgumentCaptor<TelegramBotConfig> saved = ArgumentCaptor.forClass(TelegramBotConfig.class);
        verify(repo).save(saved.capture());
        assertNotEquals("TOK", saved.getValue().getBotTokenCipher());
        assertEquals("TOK", cipher.decrypt(saved.getValue().getBotTokenCipher()));
        assertEquals(TelegramConfigService.sha256(secret.getValue()), saved.getValue().getWebhookSecretHash());
        assertEquals("leo_bot", r.botUsername());
    }

    @Test
    void saveRejectsInvalidToken() {
        when(client.getMe("BAD")).thenThrow(new TelegramBotClient.TelegramApiException("x", null));
        assertThrows(ResponseStatusException.class, () -> service.save(CO, "BAD", USER));
        verify(repo, never()).save(any());
    }

    @Test
    void saveRequiresWebhookBaseUrl() {
        service = new TelegramConfigService(client, repo, cipher, "");
        assertThrows(ResponseStatusException.class, () -> service.save(CO, "TOK", USER));
    }

    @Test
    void tokenIfValidSecretChecksHashAndEnabled() {
        TelegramBotConfig c = new TelegramBotConfig();
        c.setCompanyId(CO);
        c.setBotTokenCipher(cipher.encrypt("TOK"));
        c.setWebhookSecretHash(TelegramConfigService.sha256("S"));
        c.setEnabled(true);
        when(repo.findById(CO)).thenReturn(Optional.of(c));
        assertEquals(Optional.of("TOK"), service.tokenIfValidSecret(CO, "S"));
        assertEquals(Optional.empty(), service.tokenIfValidSecret(CO, "errado"));
        assertEquals(Optional.empty(), service.tokenIfValidSecret(CO, null));
        c.setEnabled(false);
        assertEquals(Optional.empty(), service.tokenIfValidSecret(CO, "S"));
    }

    @Test
    void deleteRemovesWebhookAndRow() {
        TelegramBotConfig c = new TelegramBotConfig();
        c.setCompanyId(CO);
        c.setBotTokenCipher(cipher.encrypt("TOK"));
        when(repo.findById(CO)).thenReturn(Optional.of(c));
        service.delete(CO);
        verify(client).deleteWebhook("TOK");
        verify(repo).delete(c);
    }
}
