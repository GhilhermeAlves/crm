package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.dto.FollowUpRequest;
import com.becommerce.crm.application.followup.dto.FollowUpResponse;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;
import com.becommerce.crm.domain.followup.FollowUpStatus;
import com.becommerce.crm.domain.followup.exception.FollowUpNotFoundException;
import com.becommerce.crm.domain.followup.exception.FollowUpValidationException;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationMode;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();

    @Mock FollowUpRepository followUpRepository;
    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks FollowUpService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private Conversation conversation(ConversationMode mode) {
        return Conversation.reconstitute(conversationId, companyId, channelId, null,
                "+5511999998888", ConversationStatus.OPEN, mode, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private FollowUp followUp(FollowUpStatus status) {
        UUID id = UUID.randomUUID();
        return FollowUp.reconstitute(id, companyId, conversationId, status, FollowUpAction.SEND_MESSAGE,
                "Obrigado pelo contato!", LocalDateTime.now().plusHours(2), 0, null, null, null, null,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    private FollowUpRequest request() {
        return new FollowUpRequest(conversationId, "Obrigado pelo contato!",
                LocalDateTime.now().plusHours(2), null, null);
    }

    @Test
    void create_shouldPersistSendMessageFollowUp() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.AUTOMATIC)));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpResponse response = service.create(companyId, request());

        assertEquals(FollowUpStatus.PENDING, response.status());
        assertEquals(FollowUpAction.SEND_MESSAGE, response.actionType());
        assertEquals("Obrigado pelo contato!", response.actionContent());
        assertNotNull(response.id());
        verify(followUpRepository).save(any(FollowUp.class));
        verify(auditor).record(eq(companyId), eq(AuditAction.CREATE), eq(AuditModule.FOLLOWUPS),
                eq("FollowUp"), anyString(), any(), isNull(), isNull());
    }

    @Test
    void create_inHumanMode_shouldReject() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.HUMAN)));

        assertThrows(FollowUpValidationException.class, () -> service.create(companyId, request()));
        verify(followUpRepository, never()).save(any());
    }

    @Test
    void create_otherCompanyConversation_shouldThrowNotFound() {
        Conversation other = Conversation.reconstitute(UUID.randomUUID(), UUID.randomUUID(), channelId, null,
                "+5511999998888", ConversationStatus.OPEN, ConversationMode.AUTOMATIC, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(any())).thenReturn(Optional.of(other));

        assertThrows(OmnichannelNotFoundException.class, () -> service.create(companyId, request()));
    }

    @Test
    void create_missingFields_shouldBeRejected() {
        assertThrows(FollowUpValidationException.class,
                () -> service.create(companyId, new FollowUpRequest(null, "msg", LocalDateTime.now(), null, null)));
        assertThrows(FollowUpValidationException.class,
                () -> service.create(companyId, new FollowUpRequest(conversationId, null, LocalDateTime.now(), null, null)));
    }

    @Test
    void create_withExistingIdempotencyKey_shouldReuseExisting() {
        FollowUp existing = followUp(FollowUpStatus.PENDING);
        UUID key = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.AUTOMATIC)));
        when(followUpRepository.findByIdempotencyKey(companyId, key)).thenReturn(Optional.of(existing));

        FollowUpResponse response = service.create(companyId,
                new FollowUpRequest(conversationId, "x", LocalDateTime.now().plusHours(1), key, null));

        assertEquals(existing.getId(), response.id());
        verify(followUpRepository, never()).save(any());
    }

    @Test
    void get_shouldReturnOwnFollowUp() {
        FollowUp f = followUp(FollowUpStatus.PENDING);
        when(followUpRepository.findById(f.getId())).thenReturn(Optional.of(f));

        FollowUpResponse response = service.get(companyId, f.getId());

        assertEquals(f.getId(), response.id());
    }

    @Test
    void get_otherCompanyFollowUp_shouldThrowNotFound() {
        FollowUp f = followUp(FollowUpStatus.PENDING);
        when(followUpRepository.findById(f.getId())).thenReturn(Optional.of(f));

        assertThrows(FollowUpNotFoundException.class, () -> service.get(UUID.randomUUID(), f.getId()));
    }

    @Test
    void get_missing_shouldThrowNotFound() {
        when(followUpRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(FollowUpNotFoundException.class, () -> service.get(companyId, UUID.randomUUID()));
    }

    @Test
    void list_shouldReturnPaged() {
        FollowUp f = followUp(FollowUpStatus.PENDING);
        when(followUpRepository.findByCompany(companyId, conversationId, 0, 20))
                .thenReturn(PageResponse.of(List.of(f), 0, 20, 1));

        PageResponse<FollowUpResponse> result = service.list(companyId, conversationId, 0, 20);

        assertEquals(1, result.totalElements());
        assertEquals(f.getId(), result.content().get(0).id());
    }

    @Test
    void cancel_pending_shouldCancelWithUserReason() {
        FollowUp f = followUp(FollowUpStatus.PENDING);
        when(followUpRepository.findById(f.getId())).thenReturn(Optional.of(f));
        when(followUpRepository.cancelPending(eq(companyId), eq(f.getId()), any())).thenReturn(true);

        FollowUpResponse response = service.cancel(companyId, f.getId());

        assertEquals(FollowUpStatus.CANCELLED, response.status());
        assertEquals(FollowUpCancellationReason.USER, response.cancelledReason());
        verify(auditor).record(eq(companyId), eq(AuditAction.UPDATE), eq(AuditModule.FOLLOWUPS),
                eq("FollowUp"), eq(f.getId().toString()), any(), isNull(), isNull());
    }

    @Test
    void cancel_sent_shouldRejectByDomain() {
        FollowUp f = followUp(FollowUpStatus.SENT);
        when(followUpRepository.findById(f.getId())).thenReturn(Optional.of(f));

        assertThrows(FollowUpValidationException.class, () -> service.cancel(companyId, f.getId()));
        verify(followUpRepository, never()).cancelPending(any(), any(), any());
    }

    @Test
    void cancel_whenRaceLoses_shouldReject() {
        FollowUp f = followUp(FollowUpStatus.PENDING);
        when(followUpRepository.findById(f.getId())).thenReturn(Optional.of(f));
        when(followUpRepository.cancelPending(any(), any(), any())).thenReturn(false);

        assertThrows(FollowUpValidationException.class, () -> service.cancel(companyId, f.getId()));
    }
}