package com.becommerce.crm.application.storage.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.company.service.CompanyQuotaService;
import com.becommerce.crm.application.storage.dto.StorageDownload;
import com.becommerce.crm.application.storage.dto.StorageResponse;
import com.becommerce.crm.application.storage.port.input.StorageUseCase;
import com.becommerce.crm.application.storage.port.output.StorageRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.storage.StorageObject;
import com.becommerce.crm.domain.storage.exception.StorageObjectNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Armazenamento (Sprint 8.6). Antes de aceitar um upload, valida a quota
 * {@code max_storage_mb} da empresa via {@link CompanyQuotaService} — impedindo
 * que qualquer endpoint ultrapasse o limite.
 */
@Service
public class StorageService implements StorageUseCase {

    private final StorageRepository storageRepository;
    private final CompanyQuotaService quotaService;
    private final TenantAuditRecorder auditor;

    public StorageService(StorageRepository storageRepository,
                          CompanyQuotaService quotaService,
                          TenantAuditRecorder auditor) {
        this.storageRepository = storageRepository;
        this.quotaService = quotaService;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public StorageResponse upload(UUID companyId, String fileName, String contentType,
                                  byte[] data, UUID userId) {
        byte[] bytes = data == null ? new byte[0] : data;
        try {
            TenantContext.setCompanyId(companyId);

            // A quota é validada pelo espaço a ser reservado (bytes reais a gravar).
            quotaService.assertCanAddSpace(companyId, bytes.length);

            StorageObject storage = StorageObject.create(
                    companyId, UUID.randomUUID().toString(), safe(fileName),
                    contentType, bytes, userId);
            StorageObject saved = storageRepository.save(storage);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.SETTINGS, "StorageObject",
                    saved.getId().toString(),
                    "Arquivo enviado: " + saved.getFileName() + " (" + saved.getSizeBytes() + " bytes)",
                    userId, Map.of("fileName", saved.getFileName(), "sizeBytes", saved.getSizeBytes()));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageResponse> list(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return storageRepository.listByCompanyId(companyId).stream()
                    .map(StorageService::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageDownload download(UUID companyId, UUID objectId) {
        try {
            TenantContext.setCompanyId(companyId);
            StorageObject object = storageRepository.findByIdAndCompanyId(objectId, companyId)
                    .orElseThrow(() -> new StorageObjectNotFoundException(objectId));
            return new StorageDownload(
                    object.getId(), object.getFileName(), object.getContentType(),
                    object.getSizeBytes(), object.getData());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID objectId) {
        try {
            TenantContext.setCompanyId(companyId);
            storageRepository.findByIdAndCompanyId(objectId, companyId)
                    .orElseThrow(() -> new StorageObjectNotFoundException(objectId));
            storageRepository.deleteByIdAndCompanyId(objectId, companyId);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.SETTINGS, "StorageObject",
                    objectId.toString(), "Arquivo removido: " + objectId, null,
                    Map.of("objectId", objectId.toString()));
        } finally {
            TenantContext.clear();
        }
    }

    private static StorageResponse toResponse(StorageObject o) {
        return new StorageResponse(
                o.getId(), o.getObjectKey(), o.getFileName(), o.getContentType(),
                o.getSizeBytes(), o.getCompanyId(), o.getCreatedAt());
    }

    private String safe(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "untitled";
        }
        String cleaned = fileName.replaceAll("[^\\p{Alnum}. _-]", "_");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}