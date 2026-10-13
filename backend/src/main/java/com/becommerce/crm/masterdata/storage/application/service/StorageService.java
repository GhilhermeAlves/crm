package com.becommerce.crm.masterdata.storage.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.service.CompanyQuotaService;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.masterdata.storage.application.port.output.StorageRepository;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.storage.domain.StorageObject;
import com.becommerce.crm.masterdata.storage.domain.exception.StorageObjectNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
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
    private final ContactRepository contactRepository;
    private final TenantAuditRecorder auditor;

    public StorageService(StorageRepository storageRepository,
                          CompanyQuotaService quotaService,
                          ContactRepository contactRepository,
                          TenantAuditRecorder auditor) {
        this.storageRepository = storageRepository;
        this.quotaService = quotaService;
        this.contactRepository = contactRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public StorageResponse upload(UUID companyId, String fileName, String contentType,
                                  byte[] data, UUID userId) {
        return doUpload(companyId, null, fileName, contentType, data, userId);
    }

    @Override
    @Transactional
    public StorageResponse uploadForContact(UUID companyId, UUID contactId, String fileName,
                                            String contentType, byte[] data, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
        } finally {
            TenantContext.clear();
        }
        return doUpload(companyId, contactId, fileName, contentType, data, userId);
    }

    private StorageResponse doUpload(UUID companyId, UUID contactId, String fileName, String contentType,
                                     byte[] data, UUID userId) {
        byte[] bytes = data == null ? new byte[0] : data;
        try {
            TenantContext.setCompanyId(companyId);

            // A quota é validada pelo espaço a ser reservado (bytes reais a gravar).
            quotaService.assertCanAddSpace(companyId, bytes.length);

            StorageObject storage = StorageObject.createForContact(
                    companyId, contactId, UUID.randomUUID().toString(), safe(fileName),
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
    public List<StorageResponse> listForContact(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            return storageRepository.listByContactId(companyId, contactId).stream()
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

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private static StorageResponse toResponse(StorageObject o) {
        return new StorageResponse(
                o.getId(), o.getObjectKey(), o.getFileName(), o.getContentType(),
                o.getSizeBytes(), o.getCompanyId(), o.getCreatedAt(), o.getContactId());
    }

    private String safe(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "untitled";
        }
        String cleaned = fileName.replaceAll("[^\\p{Alnum}. _-]", "_");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}