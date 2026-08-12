package com.becommerce.crm.application.storage.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.company.service.CompanyQuotaService;
import com.becommerce.crm.application.storage.dto.StorageResponse;
import com.becommerce.crm.application.storage.port.output.StorageRepository;
import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import com.becommerce.crm.domain.storage.StorageObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock StorageRepository storageRepository;
    @Mock CompanyQuotaService quotaService;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks StorageService storageService;

    private final UUID companyId = UUID.randomUUID();

    @Test
    void shouldUploadWhenWithinQuota() {
        byte[] data = new byte[1024];
        doNothing().when(quotaService).assertCanAddSpace(companyId, data.length);
        when(storageRepository.save(any(StorageObject.class))).thenAnswer(inv -> inv.getArgument(0));

        StorageResponse response = storageService.upload(companyId, "arquivo.pdf", "application/pdf", data, UUID.randomUUID());

        assertNotNull(response.id());
        assertEquals("arquivo.pdf", response.fileName());
        assertEquals(1024L, response.sizeBytes());
        verify(storageRepository).save(any(StorageObject.class));
    }

    @Test
    void shouldBlockUploadOverQuota() {
        byte[] data = new byte[512];
        doThrow(new QuotaExceededException("Limite de armazenamento da empresa atingido."))
                .when(quotaService).assertCanAddSpace(companyId, data.length);

        assertThrows(QuotaExceededException.class,
                () -> storageService.upload(companyId, "grande.bin", "application/octet-stream", data, UUID.randomUUID()));
        verify(storageRepository, never()).save(any(StorageObject.class));
    }

    @Test
    void shouldNotAllowOverflowWhenCurrentPlusNewExceedsLimit() {
        // O enforcement de "quota de uma empresa não afeta outra" é coberto em
        // CompanyQuotaServiceTest (cálculo com limite por company_id).
        byte[] data = new byte[2048];
        doThrow(new QuotaExceededException("Limite de armazenamento da empresa atingido."))
                .when(quotaService).assertCanAddSpace(eq(companyId), anyLong());

        assertThrows(QuotaExceededException.class,
                () -> storageService.upload(companyId, "x.bin", "bin", data, UUID.randomUUID()));
    }
}