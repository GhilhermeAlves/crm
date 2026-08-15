package com.becommerce.crm.application.storage.port.input;

import com.becommerce.crm.application.storage.dto.StorageDownload;
import com.becommerce.crm.application.storage.dto.StorageResponse;

import java.util.List;
import java.util.UUID;

/** Casos de uso de armazenamento (Sprint 8.6). */
public interface StorageUseCase {

    /**
     * Faz upload respeitando {@code max_storage_mb} da empresa. Lança
     * {@code QuotaExceededException} se o novo objeto ultrapassar a quota.
     */
    StorageResponse upload(UUID companyId, String fileName, String contentType,
                           byte[] data, UUID userId);

    /** Metadados (sem bytes) dos arquivos da empresa, mais recentes primeiro. */
    List<StorageResponse> list(UUID companyId);

    /**
     * Recupera o conteúdo de um arquivo da empresa. Lança
     * {@code StorageObjectNotFoundException} se não pertencer à empresa.
     */
    StorageDownload download(UUID companyId, UUID objectId);

    /** Remove um arquivo da empresa (no-op se não pertencer a ela). */
    void delete(UUID companyId, UUID objectId);
}