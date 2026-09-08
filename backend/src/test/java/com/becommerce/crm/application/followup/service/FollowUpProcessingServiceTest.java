package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.followup.port.output.FollowUpEventPublisher;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dispatcher de FollowUps (Sprint 23): CLAIM atômico + publish do evento de
 * execução. A validação/send do follow-up passa a ser responsabilidade do
 * {@link FollowUpExecutionService} (consumer {@code crm.followup.executor});
 * este teste cobre apenas a fronteira scheduler → fila.
 */
@ExtendWith(MockitoExtension.class)
class FollowUpProcessingServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID followUpId = UUID.randomUUID();

    @Mock FollowUpRepository followUpRepository;
    @Mock FollowUpEventPublisher eventPublisher;

    @InjectMocks FollowUpProcessingService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    @Test
    void dispatch_whenClaimFails_shouldNotPublish() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(false);

        service.dispatch(companyId, followUpId);

        verify(eventPublisher, never()).publishExecution(any());
    }

    @Test
    void dispatch_whenClaimSucceeds_shouldPublishExecutionEvent() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);

        service.dispatch(companyId, followUpId);

        verify(eventPublisher).publishExecution(argThat((FollowUpExecutionEvent e) ->
                e.companyId().equals(companyId) && e.followUpId().equals(followUpId)));
    }

    @Test
    void dispatch_whenPublishFails_shouldSwallowAndLeaveForReclaim() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("broker down"))
                .when(eventPublisher).publishExecution(any());

        service.dispatch(companyId, followUpId);

        verify(eventPublisher).publishExecution(any());
    }

    @Test
    void dispatch_shouldSetAndClearTenantContext() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        org.mockito.Mockito.doAnswer(inv -> {
            assertTenantSet();
            return null;
        }).when(eventPublisher).publishExecution(any());

        service.dispatch(companyId, followUpId);

        assertNull(TenantContext.getCompanyId(), "TenantContext deve ser limpo após o dispatch");
    }

    private void assertTenantSet() {
        if (!companyId.equals(TenantContext.getCompanyId())) {
            throw new AssertionError("TenantContext deve estar configurado durante o publish");
        }
    }
}