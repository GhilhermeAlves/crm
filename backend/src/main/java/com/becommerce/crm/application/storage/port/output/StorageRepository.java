package com.becommerce.crm.application.storage.port.output;

import com.becommerce.crm.domain.storage.StorageObject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para o armazenamento de objetos por empresa. A implementação
 * atual (Sprint 8.6) persiste os bytes em {@code storage_objects}; a interface
 * permite substituir por object-store externo (ex.: MinIO/S3) no futuro.
 *
 * <p>O isolamento por tenant é garantido pelo RLS FORCE na tabela (V037) e toda
 * consulta é escopada por {@code companyId}.
 */
public interface StorageRepository {

    StorageObject save(StorageObject storageObject);

    /** Soma de {@code size_bytes} dos objetos da empresa (uso corrente). */
    long sumSizeByCompanyId(UUID companyId);

    /** Objeto de uma empresa (download/recuperação). */
    Optional<StorageObject> findByIdAndCompanyId(UUID id, UUID companyId);

    /** Metadados (sem bytes) dos objetos da empresa, mais recentes primeiro. */
    List<StorageObject> listByCompanyId(UUID companyId);

    /** Remove o objeto de uma empresa (no-op se não pertencer a ela). */
    void deleteByIdAndCompanyId(UUID id, UUID companyId);
}