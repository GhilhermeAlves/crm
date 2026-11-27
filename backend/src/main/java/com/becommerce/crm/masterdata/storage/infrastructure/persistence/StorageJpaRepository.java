package com.becommerce.crm.masterdata.storage.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageJpaRepository extends JpaRepository<StorageJpaEntity, UUID> {

    /** Uso da cota: arquivos temporários (exportações de relatório) não contam. */
    @Query("select coalesce(sum(s.sizeBytes), 0) from StorageJpaEntity s"
            + " where s.companyId = :companyId and s.temporary = false")
    Long sumSizeByCompanyId(@Param("companyId") UUID companyId);

    Optional<StorageJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    /**
     * Lista metadados (sem {@code data}) dos arquivos da empresa (sem paciente), mais recentes
     * primeiro. Temporários (exportações de relatório) ficam de fora.
     */
    @Query("""
            select s.id as id, s.objectKey as objectKey, s.fileName as fileName,
                   s.contentType as contentType, s.sizeBytes as sizeBytes, s.createdAt as createdAt
            from StorageJpaEntity s
            where s.companyId = :companyId and s.contactId is null and s.temporary = false
            order by s.createdAt desc
            """)
    List<StorageSummary> findSummariesByCompanyId(@Param("companyId") UUID companyId);

    /** Lista metadados dos arquivos vinculados a um paciente, mais recentes primeiro. */
    @Query("""
            select s.id as id, s.objectKey as objectKey, s.fileName as fileName,
                   s.contentType as contentType, s.sizeBytes as sizeBytes, s.createdAt as createdAt
            from StorageJpaEntity s
            where s.companyId = :companyId and s.contactId = :contactId
            order by s.createdAt desc
            """)
    List<StorageSummary> findSummariesByContactId(@Param("companyId") UUID companyId,
                                                  @Param("contactId") UUID contactId);

    @Modifying
    @Query("delete from StorageJpaEntity s where s.companyId = :companyId and s.id = :id")
    int deleteByCompanyIdAndId(@Param("companyId") UUID companyId, @Param("id") UUID id);

    interface StorageSummary {
        UUID getId();
        String getObjectKey();
        String getFileName();
        String getContentType();
        long getSizeBytes();
        LocalDateTime getCreatedAt();
    }
}
