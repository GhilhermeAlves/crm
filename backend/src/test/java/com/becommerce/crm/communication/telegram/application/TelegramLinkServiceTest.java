package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.communication.telegram.domain.TelegramLink;
import com.becommerce.crm.communication.telegram.domain.TelegramLinkCode;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramLinkCodeJpaRepository;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramLinkJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramLinkServiceTest {

    static final UUID CO = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @Mock TelegramLinkJpaRepository links;
    @Mock TelegramLinkCodeJpaRepository codes;
    @Mock TenantAuditRecorder auditor;
    TelegramLinkService service;

    @BeforeEach
    void setUp() {
        service = new TelegramLinkService(links, codes, auditor, Clock.systemUTC());
    }

    @Test
    void generateCodeHasSixDigitsAndStoresOnlyHash() {
        ArgumentCaptor<TelegramLinkCode> saved = ArgumentCaptor.forClass(TelegramLinkCode.class);
        String code = service.generateCode(CO, USER);
        assertTrue(code.matches("\\d{6}"));
        verify(codes).deleteByCompanyIdAndUserId(CO, USER);
        verify(codes).save(saved.capture());
        assertEquals(TelegramConfigService.sha256(CO + ":" + code), saved.getValue().getCodeHash());
        assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(9)));
    }

    @Test
    void linkWithValidCodeCreatesLinkAndBurnsCode() {
        TelegramLinkCode c = code(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), null);
        when(codes.findById(TelegramConfigService.sha256(CO + ":123456"))).thenReturn(Optional.of(c));
        when(links.findByCompanyIdAndTelegramUserId(CO, 99L)).thenReturn(Optional.empty());
        when(links.findByCompanyIdAndUserId(CO, USER)).thenReturn(Optional.empty());

        assertEquals(TelegramLinkService.LinkResult.LINKED, service.link(CO, "123456", 99L, 99L, "Gui"));
        assertNotNull(c.getUsedAt());
        verify(links).save(argThat(l -> l.getUserId().equals(USER) && l.getTelegramUserId() == 99L));
    }

    @Test
    void expiredUsedOrUnknownCodeIsRejected() {
        when(codes.findById(any())).thenReturn(Optional.empty());
        assertEquals(TelegramLinkService.LinkResult.INVALID_CODE, service.link(CO, "000000", 1, 1, "x"));

        TelegramLinkCode expired = code(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), null);
        when(codes.findById(TelegramConfigService.sha256(CO + ":111111"))).thenReturn(Optional.of(expired));
        assertEquals(TelegramLinkService.LinkResult.INVALID_CODE, service.link(CO, "111111", 1, 1, "x"));

        TelegramLinkCode used = code(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), LocalDateTime.now());
        when(codes.findById(TelegramConfigService.sha256(CO + ":222222"))).thenReturn(Optional.of(used));
        assertEquals(TelegramLinkService.LinkResult.INVALID_CODE, service.link(CO, "222222", 1, 1, "x"));
        verify(links, never()).save(any());
    }

    @Test
    void codeFromOtherCompanyDoesNotMatch() {
        // O hash inclui o companyId: o mesmo código em outra clínica gera outro hash.
        assertNotEquals(TelegramConfigService.sha256(CO + ":123456"),
                TelegramConfigService.sha256(UUID.randomUUID() + ":123456"));
    }

    @Test
    void telegramAlreadyLinkedToOtherUserIsRejected() {
        TelegramLinkCode c = code(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), null);
        when(codes.findById(any())).thenReturn(Optional.of(c));
        TelegramLink other = new TelegramLink();
        other.setUserId(UUID.randomUUID());
        when(links.findByCompanyIdAndTelegramUserId(CO, 99L)).thenReturn(Optional.of(other));
        assertEquals(TelegramLinkService.LinkResult.TELEGRAM_ALREADY_LINKED,
                service.link(CO, "123456", 99L, 99L, "x"));
    }

    private TelegramLinkCode code(LocalDateTime expires, LocalDateTime used) {
        TelegramLinkCode c = new TelegramLinkCode();
        c.setCompanyId(CO);
        c.setUserId(USER);
        c.setExpiresAt(expires);
        c.setUsedAt(used);
        return c;
    }
}
