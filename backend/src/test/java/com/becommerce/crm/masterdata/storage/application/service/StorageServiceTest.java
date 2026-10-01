package com.becommerce.crm.masterdata.storage.application.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.service.CompanyQuotaService;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.output.StorageRepository;
import com.becommerce.crm.masterdata.quota.domain.exception.QuotaExceededException;
import com.becommerce.crm.masterdata.storage.domain.StorageObject;
import com.becommerce.crm.masterdata.storage.domain.exception.StorageObjectNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void shouldListObjectsOfCompany() {
        StorageObject obj = StorageObject.reconstitute(
                UUID.randomUUID(), companyId, "k", "a.txt", "text/plain", 3, null, UUID.randomUUID(), null);
        when(storageRepository.listByCompanyId(companyId)).thenReturn(List.of(obj));

        List<StorageResponse> result = storageService.list(companyId);

        assertEquals(1, result.size());
        assertEquals(obj.getId(), result.get(0).id());
        assertEquals("a.txt", result.get(0).fileName());
        verify(storageRepository).listByCompanyId(companyId);
    }

    @Test
    void shouldDownloadObjectOfCompany() {
        UUID objectId = UUID.randomUUID();
        byte[] bytes = "conteudo".getBytes();
        StorageObject obj = StorageObject.reconstitute(
                objectId, companyId, "k", "a.txt", "text/plain", bytes.length, bytes, UUID.randomUUID(), null);
        when(storageRepository.findByIdAndCompanyId(objectId, companyId)).thenReturn(Optional.of(obj));

        StorageDownload result = storageService.download(companyId, objectId);

        assertEquals(objectId, result.id());
        assertEquals("a.txt", result.fileName());
        assertArrayEquals(bytes, result.data());
        verify(storageRepository).findByIdAndCompanyId(objectId, companyId);
    }

    @Test
    void shouldThrowWhenDownloadingObjectNotInCompany() {
        UUID objectId = UUID.randomUUID();
        when(storageRepository.findByIdAndCompanyId(objectId, companyId)).thenReturn(Optional.empty());

        assertThrows(StorageObjectNotFoundException.class, () -> storageService.download(companyId, objectId));
    }

    @Test
    void shouldDeleteObjectOfCompany() {
        UUID objectId = UUID.randomUUID();
        when(storageRepository.findByIdAndCompanyId(objectId, companyId))
                .thenReturn(Optional.of(StorageObject.reconstitute(
                        objectId, companyId, "k", "a.txt", "text/plain", 3, new byte[3], UUID.randomUUID(), null)));

        storageService.delete(companyId, objectId);

        verify(storageRepository).deleteByIdAndCompanyId(objectId, companyId);
    }

    @Test
    void shouldThrowWhenDeletingObjectNotInCompany() {
        UUID objectId = UUID.randomUUID();
        when(storageRepository.findByIdAndCompanyId(objectId, companyId)).thenReturn(Optional.empty());

        assertThrows(StorageObjectNotFoundException.class, () -> storageService.delete(companyId, objectId));
        verify(storageRepository, never()).deleteByIdAndCompanyId(any(), any());
    }
}