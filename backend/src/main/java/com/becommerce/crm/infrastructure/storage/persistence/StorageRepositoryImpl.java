package com.becommerce.crm.infrastructure.storage.persistence;

import com.becommerce.crm.application.storage.port.output.StorageRepository;
import com.becommerce.crm.domain.storage.StorageObject;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class StorageRepositoryImpl implements StorageRepository {

    private final StorageJpaRepository jpaRepository;

    public StorageRepositoryImpl(StorageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StorageObject save(StorageObject storageObject) {
        return toDomain(jpaRepository.save(toEntity(storageObject)));
    }

    @Override
    public long sumSizeByCompanyId(UUID companyId) {
        Long sum = jpaRepository.sumSizeByCompanyId(companyId);
        return sum == null ? 0L : sum;
    }

    private static StorageJpaEntity toEntity(StorageObject o) {
        StorageJpaEntity e = new StorageJpaEntity();
        e.setId(o.getId());
        e.setCompanyId(o.getCompanyId());
        e.setObjectKey(o.getObjectKey());
        e.setFileName(o.getFileName());
        e.setContentType(o.getContentType());
        e.setSizeBytes(o.getSizeBytes());
        e.setData(o.getData());
        e.setCreatedBy(o.getCreatedBy());
        e.setCreatedAt(o.getCreatedAt());
        return e;
    }

    private static StorageObject toDomain(StorageJpaEntity e) {
        return StorageObject.reconstitute(
                e.getId(), e.getCompanyId(), e.getObjectKey(), e.getFileName(),
                e.getContentType(), e.getSizeBytes(), e.getData(),
                e.getCreatedBy(), e.getCreatedAt());
    }
}