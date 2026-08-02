package com.becommerce.crm.infrastructure.audit.listener;

import com.becommerce.crm.application.audit.service.AuditService;
import com.becommerce.crm.domain.audit.AuditLog;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditEventListener auditEventListener;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSetTenantContextBeforeAuditInsert() {
        UUID companyId = UUID.randomUUID();
        UserCreatedEvent event = UserCreatedEvent.create(
                UUID.randomUUID(), "test@test.com", companyId
        );

        auditEventListener.handleUserCreated(event);

        verify(auditService).recordAudit(auditLogCaptor.capture());
        AuditLog captured = auditLogCaptor.getValue();
        assertEquals(companyId, captured.getCompanyId());
        assertFalse(TenantContext.hasCompanyId(), "TenantContext should be cleared after audit");
    }

    @Test
    void shouldClearTenantContextAfterAuditInsert() {
        UUID companyId = UUID.randomUUID();
        UserCreatedEvent event = UserCreatedEvent.create(
                UUID.randomUUID(), "test@test.com", companyId
        );

        auditEventListener.handleUserCreated(event);

        assertFalse(TenantContext.hasCompanyId());
    }

    @Test
    void shouldSetCorrectTenantForDifferentCompanies() {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();

        UserCreatedEvent eventA = UserCreatedEvent.create(
                UUID.randomUUID(), "userA@test.com", companyA
        );
        UserCreatedEvent eventB = UserCreatedEvent.create(
                UUID.randomUUID(), "userB@test.com", companyB
        );

        auditEventListener.handleUserCreated(eventA);
        auditEventListener.handleUserCreated(eventB);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService, times(2)).recordAudit(captor.capture());

        assertEquals(companyA, captor.getAllValues().get(0).getCompanyId());
        assertEquals(companyB, captor.getAllValues().get(1).getCompanyId());
    }

    @Test
    void shouldNotLeakTenantBetweenSequentialEvents() {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();

        UserCreatedEvent eventA = UserCreatedEvent.create(
                UUID.randomUUID(), "userA@test.com", companyA
        );
        UserCreatedEvent eventB = UserCreatedEvent.create(
                UUID.randomUUID(), "userB@test.com", companyB
        );

        auditEventListener.handleUserCreated(eventA);
        assertFalse(TenantContext.hasCompanyId(), "Context should be cleared after event A");

        auditEventListener.handleUserCreated(eventB);
        assertFalse(TenantContext.hasCompanyId(), "Context should be cleared after event B");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService, times(2)).recordAudit(captor.capture());

        assertEquals(companyA, captor.getAllValues().get(0).getCompanyId());
        assertEquals(companyB, captor.getAllValues().get(1).getCompanyId());
    }
}
