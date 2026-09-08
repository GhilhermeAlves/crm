package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Desfecho de follow-up assíncrono (Sprint 23): o sender consumer confirma o
 * resultado do provider e o {@link FollowUpSendOutcomeHandler} aplica
 * SENT / retry com backoff / FAILED terminal no banco.
 */
@ExtendWith(MockitoExtension.class)
class FollowUpSendOutcomeHandlerTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID followUpId = UUID.randomUUID();

    @Mock FollowUpRepository followUpRepository;
    @Mock TenantAuditRecorder auditor;

    @Test
    void markSent_shouldMarkSentAndAudit() {
        when(followUpRepository.markSent(eq(companyId), eq(followUpId), eq("wamid-1"), any()))
                .thenReturn(true);

        new FollowUpSendOutcomeHandler(followUpRepository, auditor)
                .markSent(companyId, followUpId, "wamid-1");

        verify(auditor).record(eq(companyId), any(), any(), eq("FollowUp"),
                eq(followUpId.toString()), any(), any(), any());
    }

    @Test
    void onSendFailed_belowMaxAttempts_shouldScheduleRetryWithProgressiveBackoff() {
        FollowUp f = followUp(0);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(f));

        new FollowUpSendOutcomeHandler(followUpRepository, auditor)
                .onSendFailed(companyId, followUpId, "provider down");

        verify(followUpRepository).scheduleRetry(eq(companyId), eq(followUpId),
                argThat(at -> at.isAfter(LocalDateTime.now()) && at.isBefore(LocalDateTime.now().plusMinutes(30))),
                eq("provider down"), any());
        verify(followUpRepository, never()).markFailedTerminal(any(), any(), any(), any());
    }

    @Test
    void onSendFailed_atMaxAttempts_shouldMarkFailedTerminal() {
        FollowUp f = followUp(2);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(f));

        new FollowUpSendOutcomeHandler(followUpRepository, auditor)
                .onSendFailed(companyId, followUpId, "provider down");

        verify(followUpRepository).markFailedTerminal(eq(companyId), eq(followUpId),
                eq("provider down"), any());
        verify(followUpRepository, never()).scheduleRetry(any(), any(), any(), any(), any());
    }

    @Test
    void onSendFailed_missingOrForeignFollowUp_shouldDoNothing() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.empty());

        new FollowUpSendOutcomeHandler(followUpRepository, auditor)
                .onSendFailed(companyId, followUpId, "x");

        verify(followUpRepository, never()).scheduleRetry(any(), any(), any(), any(), any());
        verify(followUpRepository, never()).markFailedTerminal(any(), any(), any(), any());
    }

    @Test
    void backoff_shouldBeProgressive() {
        assertTrue(FollowUpSendOutcomeHandler.backoff(1).toMinutes()
                < FollowUpSendOutcomeHandler.backoff(2).toMinutes());
        assertTrue(FollowUpSendOutcomeHandler.backoff(2).toMinutes()
                < FollowUpSendOutcomeHandler.backoff(3).toMinutes());
        assertEquals(15, FollowUpSendOutcomeHandler.backoff(1).toMinutes());
        assertEquals(60, FollowUpSendOutcomeHandler.backoff(2).toMinutes());
        assertEquals(240, FollowUpSendOutcomeHandler.backoff(3).toMinutes());
        assertEquals(240, FollowUpSendOutcomeHandler.backoff(9).toMinutes());
    }

    private FollowUp followUp(int attempts) {
        return FollowUp.reconstitute(followUpId, companyId, UUID.randomUUID(), FollowUpStatus.PROCESSING,
                FollowUpAction.SEND_MESSAGE, "x", LocalDateTime.now(), attempts, "provider down", null,
                LocalDateTime.now(), null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}