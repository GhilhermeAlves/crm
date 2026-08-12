package com.becommerce.crm.application.storage.port.output;

import com.becommerce.crm.domain.storage.StorageObject;

import java.util.UUID;

/**
 * Porta de saída para o armazenamento de objetos por empresa. A implementação
 * atual (Sprint 8.6) persiste os bytes em {@code storage_objects}; a interface
 * permite substituir por object-store externo (ex.: MinIO/S3) no futuro.
 *
 * <p>O isolamento por tenant é garantido pelo RLS FORCE na tabela (V037).
 */
public interface StorageRepository {

    StorageObject save(StorageObject storageObject);

    /** Soma de {@code size_bytes} dos objetos da empresa (uso corrente). */
    long sumSizeByCompanyId(UUID companyId);
}