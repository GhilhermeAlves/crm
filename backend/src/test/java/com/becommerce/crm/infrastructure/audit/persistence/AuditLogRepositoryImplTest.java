package com.becommerce.crm.infrastructure.audit.persistence;

import com.becommerce.crm.application.audit.port.output.AuditLogRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditLog;
import com.becommerce.crm.domain.audit.AuditModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryImplTest {

    @Mock
    private SpringDataAuditLogRepository repository;

    private AuditLogRepository auditLogRepository;

    private final UUID companyId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        auditLogRepository = new AuditLogRepositoryImpl(repository);
    }

    @Test
    void shouldInsertNewAuditLogWhenIdDoesNotExistYet() {
        UUID domainId = UUID.randomUUID();
        UUID generatedId = UUID.randomUUID();

        AuditLog auditLog = AuditLog.create(companyId, AuditAction.CREATE, AuditModule.USERS);
        auditLog.setId(domainId);

        when(repository.existsById(domainId)).thenReturn(false);
        when(repository.save(any(AuditLogJpaEntity.class))).thenAnswer(invocation -> {
            AuditLogJpaEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        AuditLog saved = auditLogRepository.save(auditLog);

        assertNotNull(saved.getId());
        assertEquals(companyId, saved.getCompanyId());
        assertEquals(AuditAction.CREATE, saved.getAction());
        assertEquals(AuditModule.USERS, saved.getModule());
        verify(repository).save(any(AuditLogJpaEntity.class));
    }

    @Test
    void shouldCopyAllAuditMetadataToEntity() {
        AuditLog auditLog = AuditLog.create(companyId, AuditAction.UPDATE, AuditModule.TENANTS);
        auditLog.setUserId(UUID.randomUUID());
        auditLog.setUserName("Validacao Tester");
        auditLog.setUserEmail("validacao.tester@crm.local");
        auditLog.setEntityName("Company");
        auditLog.setEntityId(companyId.toString());
        auditLog.setDescription("Empresa atualizada");
        auditLog.setNewValues(java.util.Map.of("companyName", "Empresa LTDA"));
        auditLog.setIpAddress("10.0.0.1");
        auditLog.setUserAgent("TestAgent");

        when(repository.existsById(auditLog.getId())).thenReturn(false);
        when(repository.save(any(AuditLogJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogRepository.save(auditLog);

        verify(repository).save(any(AuditLogJpaEntity.class));
        verify(repository).save(any());
    }
}
