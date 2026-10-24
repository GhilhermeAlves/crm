package com.becommerce.crm.clinical.trichoscopy.infrastructure.persistence;

import com.becommerce.crm.clinical.trichoscopy.application.port.out.TrichoscopyRepository;
import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TrichoscopyRepositoryAdapter implements TrichoscopyRepository {

    private final SpringDataTrichoscopyRepository repository;

    public TrichoscopyRepositoryAdapter(SpringDataTrichoscopyRepository repository) {
        this.repository = repository;
    }

    @Override
    public PatientTrichoscopyImage save(PatientTrichoscopyImage image) {
        return toDomain(repository.save(toEntity(image)));
    }

    @Override
    public List<PatientTrichoscopyImage> findByCompanyIdAndContactId(UUID companyId, UUID contactId) {
        return repository.findByCompanyIdAndContactIdOrderByTakenAtDesc(companyId, contactId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PatientTrichoscopyImage> findByIdAndCompanyIdAndContactId(UUID id, UUID companyId,
                                                                             UUID contactId) {
        return repository.findByIdAndCompanyIdAndContactId(id, companyId, contactId).map(this::toDomain);
    }

    @Override
    public void delete(PatientTrichoscopyImage image) {
        repository.deleteById(image.getId());
    }

    private PatientTrichoscopyImageJpaEntity toEntity(PatientTrichoscopyImage image) {
        PatientTrichoscopyImageJpaEntity entity = new PatientTrichoscopyImageJpaEntity();
        entity.setId(image.getId());
        entity.setCompanyId(image.getCompanyId());
        entity.setContactId(image.getContactId());
        entity.setStorageObjectId(image.getStorageObjectId());
        entity.setTakenBy(image.getTakenBy());
        entity.setTakenAt(image.getTakenAt());
        entity.setNotes(image.getNotes());
        entity.setCreatedAt(image.getCreatedAt());
        return entity;
    }

    private PatientTrichoscopyImage toDomain(PatientTrichoscopyImageJpaEntity entity) {
        return PatientTrichoscopyImage.reconstitute(
                entity.getId(),
                entity.getCompanyId(),
                entity.getContactId(),
                entity.getStorageObjectId(),
                entity.getTakenBy(),
                entity.getTakenAt(),
                entity.getNotes(),
                entity.getCreatedAt());
    }
}
