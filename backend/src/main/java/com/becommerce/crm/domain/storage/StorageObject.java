package com.becommerce.crm.domain.storage;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Objeto de armazenamento por empresa (Sprint 8.6). Rico em tamanho em bytes
 * para permitir o cálculo do uso corrente frente a {@code max_storage_mb} do
 * plano da empresa.
 */
public class StorageObject {

    private final UUID id;
    private final UUID companyId;
    private final String objectKey;
    private final String fileName;
    private final String contentType;
    private final long sizeBytes;
    private final byte[] data;
    private final UUID createdBy;
    private final LocalDateTime createdAt;

    private StorageObject(UUID id, UUID companyId, String objectKey, String fileName,
                          String contentType, long sizeBytes, byte[] data,
                          UUID createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.objectKey = objectKey;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.data = data;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static StorageObject create(UUID companyId, String objectKey, String fileName,
                                       String contentType, byte[] data, UUID createdBy) {
        return new StorageObject(
                UUID.randomUUID(), companyId, objectKey, fileName, contentType,
                data == null ? 0L : data.length, data, createdBy, LocalDateTime.now());
    }

    public static StorageObject reconstitute(UUID id, UUID companyId, String objectKey, String fileName,
                                             String contentType, long sizeBytes, byte[] data,
                                             UUID createdBy, LocalDateTime createdAt) {
        return new StorageObject(
                id, companyId, objectKey, fileName, contentType, sizeBytes, data, createdBy, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getObjectKey() { return objectKey; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public byte[] getData() { return data; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}