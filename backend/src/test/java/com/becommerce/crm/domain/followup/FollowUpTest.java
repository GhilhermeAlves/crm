package com.becommerce.crm.domain.followup;

import com.becommerce.crm.domain.followup.exception.FollowUpValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    private FollowUp pending() {
        return FollowUp.create(companyId, conversationId, FollowUpAction.SEND_MESSAGE, "Podemos conversar amanhã?",
                LocalDateTime.now().plusHours(1), null);
    }

    @Test
    void create_shouldStartAsPending() {
        FollowUp f = pending();
        assertEquals(FollowUpStatus.PENDING, f.getStatus());
        assertEquals(FollowUpAction.SEND_MESSAGE, f.getActionType());
        assertEquals(0, f.getAttempts());
        assertNull(f.getCancelledReason());
        assertNull(f.getIdempotencyKey());
    }

    @Test
    void create_shouldKeepIdempotencyKey() {
        UUID key = UUID.randomUUID();
        FollowUp f = FollowUp.create(companyId, conversationId, FollowUpAction.SEND_MESSAGE, "msg",
                LocalDateTime.now().plusHours(1), key);
        assertEquals(key, f.getIdempotencyKey());
    }

    @Test
    void create_shouldRejectBlankContent() {
        assertThrows(FollowUpValidationException.class, () -> FollowUp.create(companyId, conversationId,
                FollowUpAction.SEND_MESSAGE, "   ", LocalDateTime.now().plusHours(1), null));
    }

    @Test
    void create_shouldRejectEmptyContent() {
        assertThrows(FollowUpValidationException.class, () -> FollowUp.create(companyId, conversationId,
                FollowUpAction.SEND_MESSAGE, "", LocalDateTime.now().plusHours(1), null));
    }

    @Test
    void create_shouldRejectNullAction() {
        assertThrows(FollowUpValidationException.class, () -> FollowUp.create(companyId, conversationId,
                null, "msg", LocalDateTime.now().plusHours(1), null));
    }

    @Test
    void create_shouldRejectNullExecuteAt() {
        assertThrows(FollowUpValidationException.class, () -> FollowUp.create(companyId, conversationId,
                FollowUpAction.SEND_MESSAGE, "msg", null, null));
    }

    @Test
    void cancel_fromPending_shouldCancelWithUserReason() {
        FollowUp f = pending();
        f.cancel();
        assertEquals(FollowUpStatus.CANCELLED, f.getStatus());
        assertEquals(FollowUpCancellationReason.USER, f.getCancelledReason());
        assertNotNull(f.getCancelledAt());
    }

    @Test
    void cancel_fromSent_shouldThrow() {
        FollowUp f = pending();
        f.markProcessing();
        f.markSent("wamid-1");
        assertThrows(FollowUpValidationException.class, f::cancel);
    }

    @Test
    void markProcessing_shouldRequirePending() {
        FollowUp f = pending();
        f.markProcessing();
        assertThrows(FollowUpValidationException.class, f::markProcessing);
    }

    @Test
    void markSent_shouldRequireProcessing() {
        FollowUp f = pending();
        assertThrows(FollowUpValidationException.class, () -> f.markSent("wamid-1"));
    }

    @Test
    void lifecycle_pendingProcessingSent() {
        FollowUp f = pending();
        f.markProcessing();
        assertEquals(FollowUpStatus.PROCESSING, f.getStatus());
        assertNotNull(f.getProcessingStartedAt());
        f.markSent("wamid-123");
        assertEquals(FollowUpStatus.SENT, f.getStatus());
        assertEquals("wamid-123", f.getResultText());
        assertNotNull(f.getProcessedAt());
    }

    @Test
    void markFailed_shouldRequireProcessing() {
        FollowUp f = pending();
        assertThrows(FollowUpValidationException.class,
                () -> f.markFailed("erro"));
        f.markProcessing();
        f.markFailed("falha terminal");
        assertEquals(FollowUpStatus.FAILED, f.getStatus());
        assertEquals("falha terminal", f.getLastError());
    }

    @Test
    void scheduleRetry_shouldRequeueWithBackoffAndIncrementAttempts() {
        FollowUp f = pending();
        f.markProcessing();
        LocalDateTime next = LocalDateTime.now().plusMinutes(15);
        f.scheduleRetry("falha transitória", next);
        assertEquals(FollowUpStatus.PENDING, f.getStatus());
        assertEquals(1, f.getAttempts());
        assertEquals(next, f.getExecuteAt());
        assertEquals("falha transitória", f.getLastError());
        assertNull(f.getProcessingStartedAt());
    }

    @Test
    void scheduleRetry_beyondMaxAttempts_shouldThrow() {
        FollowUp f = pending();
        for (int i = 0; i < 3; i++) {
            f.markProcessing();
            f.scheduleRetry("falha", LocalDateTime.now().plusMinutes(15));
        }
        assertThrows(FollowUpValidationException.class,
                () -> f.scheduleRetry("falha", LocalDateTime.now().plusMinutes(15)));
    }

    @Test
    void markCancelledByRule_fromProcessing_shouldCancelWithReason() {
        FollowUp f = pending();
        f.markProcessing();
        f.markCancelledByRule(FollowUpCancellationReason.HUMAN_MODE);
        assertEquals(FollowUpStatus.CANCELLED, f.getStatus());
        assertEquals(FollowUpCancellationReason.HUMAN_MODE, f.getCancelledReason());
    }

    @Test
    void markCancelledByRule_fromPendingWithStaleReason() {
        FollowUp f = pending();
        f.markCancelledByRule(FollowUpCancellationReason.SUPERSEDED_BY_NEW_MESSAGE);
        assertEquals(FollowUpStatus.CANCELLED, f.getStatus());
        assertEquals(FollowUpCancellationReason.SUPERSEDED_BY_NEW_MESSAGE, f.getCancelledReason());
    }

    @Test
    void markCancelledByRule_afterSent_shouldThrow() {
        FollowUp f = pending();
        f.markProcessing();
        f.markSent("wamid-1");
        assertThrows(FollowUpValidationException.class,
                () -> f.markCancelledByRule(FollowUpCancellationReason.HUMAN_MODE));
    }
}