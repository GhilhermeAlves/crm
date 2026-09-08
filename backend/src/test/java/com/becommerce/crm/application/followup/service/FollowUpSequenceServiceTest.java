package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceRequest;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceResponse;
import com.becommerce.crm.application.followup.port.output.FollowUpSequenceRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUpSequence;
import com.becommerce.crm.domain.followup.FollowUpSequenceStatus;
import com.becommerce.crm.domain.followup.exception.FollowUpSequenceNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpSequenceServiceTest {

    private final UUID companyId = UUID.randomUUID();

    @Mock FollowUpSequenceRepository sequenceRepository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks FollowUpSequenceService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private FollowUpSequence sequence(FollowUpSequenceStatus status) {
        UUID id = UUID.randomUUID();
        return FollowUpSequence.reconstitute(id, companyId, "Carrinho abandonado", "Recuperação",
                status, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    private FollowUpSequenceRequest request() {
        return new FollowUpSequenceRequest("Carrinho abandonado", "Recuperação");
    }

    @Test
    void create_shouldPersistActiveSequence() {
        when(sequenceRepository.save(any(FollowUpSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpSequenceResponse response = service.create(companyId, request());

        assertEquals("Carrinho abandonado", response.name());
        assertEquals(FollowUpSequenceStatus.ACTIVE, response.status());
        assertNotNull(response.id());
        verify(sequenceRepository).save(any(FollowUpSequence.class));
        verify(auditor).record(eq(companyId), eq(AuditAction.CREATE), eq(AuditModule.FOLLOWUPS),
                eq("FollowUpSequence"), anyString(), any(), isNull(), isNull());
    }

    @Test
    void get_shouldReturnOwnSequence() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));

        FollowUpSequenceResponse response = service.get(companyId, s.getId());

        assertEquals(s.getId(), response.id());
    }

    @Test
    void get_otherCompanySequence_shouldThrowNotFound() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));

        assertThrows(FollowUpSequenceNotFoundException.class,
                () -> service.get(UUID.randomUUID(), s.getId()));
    }

    @Test
    void get_missing_shouldThrowNotFound() {
        when(sequenceRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(FollowUpSequenceNotFoundException.class, () -> service.get(companyId, UUID.randomUUID()));
    }

    @Test
    void list_shouldReturnPaged() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findByCompany(companyId, 0, 20))
                .thenReturn(PageResponse.of(List.of(s), 0, 20, 1));

        PageResponse<FollowUpSequenceResponse> result = service.list(companyId, 0, 20);

        assertEquals(1, result.totalElements());
        assertEquals(s.getId(), result.content().get(0).id());
    }

    @Test
    void update_shouldPersistChanges() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(sequenceRepository.save(any(FollowUpSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpSequenceResponse response = service.update(companyId, s.getId(),
                new FollowUpSequenceRequest("Novo nome", "Nova desc"));

        assertEquals("Novo nome", response.name());
        assertEquals("Nova desc", response.description());
        verify(auditor).record(eq(companyId), eq(AuditAction.UPDATE), eq(AuditModule.FOLLOWUPS),
                eq("FollowUpSequence"), eq(s.getId().toString()), any(), isNull(), isNull());
    }

    @Test
    void delete_shouldDeleteOwnedSequence() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));

        service.delete(companyId, s.getId());

        verify(sequenceRepository).delete(companyId, s.getId());
        verify(auditor).record(eq(companyId), eq(AuditAction.DELETE), eq(AuditModule.FOLLOWUPS),
                eq("FollowUpSequence"), anyString(), any(), isNull(), isNull());
    }

    @Test
    void delete_otherCompanySequence_shouldThrowNotFoundAndNotDelete() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));

        assertThrows(FollowUpSequenceNotFoundException.class,
                () -> service.delete(UUID.randomUUID(), s.getId()));
        verify(sequenceRepository, never()).delete(any(), any());
    }

    @Test
    void activate_shouldActivate() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.INACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(sequenceRepository.save(any(FollowUpSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpSequenceResponse response = service.activate(companyId, s.getId());

        assertEquals(FollowUpSequenceStatus.ACTIVE, response.status());
    }

    @Test
    void deactivate_shouldDeactivate() {
        FollowUpSequence s = sequence(FollowUpSequenceStatus.ACTIVE);
        when(sequenceRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(sequenceRepository.save(any(FollowUpSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpSequenceResponse response = service.deactivate(companyId, s.getId());

        assertEquals(FollowUpSequenceStatus.INACTIVE, response.status());
    }
}