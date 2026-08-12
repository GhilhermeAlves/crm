package com.becommerce.crm.application.storage.port.input;

import com.becommerce.crm.application.storage.dto.StorageResponse;

import java.util.UUID;

/** Casos de uso de armazenamento (Sprint 8.6). */
public interface StorageUseCase {

    /**
     * Faz upload respeitando {@code max_storage_mb} da empresa. Lança
     * {@code QuotaExceededException} se o novo objeto ultrapassar a quota.
     */
    StorageResponse upload(UUID companyId, String fileName, String contentType,
                           byte[] data, UUID userId);
}