package com.becommerce.crm.clinical.anamnesis.infrastructure.persistence;

import com.becommerce.crm.clinical.anamnesis.application.port.out.PatientAnamnesisRepository;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesis;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PatientAnamnesisRepositoryImpl implements PatientAnamnesisRepository {

    private final PatientAnamnesisJpaRepository jpaRepository;

    public PatientAnamnesisRepositoryImpl(PatientAnamnesisJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PatientAnamnesis save(PatientAnamnesis anamnesis) {
        return toDomain(jpaRepository.save(toEntity(anamnesis)));
    }

    @Override
    public Optional<PatientAnamnesis> findById(UUID id) {
        return jpaRepository.findById(id).map(PatientAnamnesisRepositoryImpl::toDomain);
    }

    @Override
    public List<PatientAnamnesis> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId,
                                                                                  UUID contactId) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId).stream()
                .map(PatientAnamnesisRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<PatientAnamnesis> findFirstByCompanyIdAndContactIdAndStatusOrderByCompletedAtDesc(
            UUID companyId, UUID contactId, PatientAnamnesisStatus status) {
        return jpaRepository.findFirstByCompanyIdAndContactIdAndStatusOrderByCompletedAtDesc(
                companyId, contactId, status.name()).map(PatientAnamnesisRepositoryImpl::toDomain);
    }

    @Override
    public void delete(PatientAnamnesis anamnesis) {
        jpaRepository.deleteById(anamnesis.getId());
    }

    private static PatientAnamnesisJpaEntity toEntity(PatientAnamnesis anamnesis) {
        PatientAnamnesisJpaEntity e = new PatientAnamnesisJpaEntity();
        e.setId(anamnesis.getId());
        e.setCompanyId(anamnesis.getCompanyId());
        e.setContactId(anamnesis.getContactId());
        e.setModelId(anamnesis.getModelId());
        e.setModelName(anamnesis.getModelName());
        e.setModelVersion(anamnesis.getModelVersion());
        e.setSnapshot(anamnesis.getSnapshot());
        e.setAnswers(anamnesis.getAnswers());
        e.setAlerts(anamnesis.getAlerts());
        e.setStatus(anamnesis.getStatus().name());
        e.setCreatedBy(anamnesis.getCreatedBy());
        e.setCompletedBy(anamnesis.getCompletedBy());
        e.setCompletedAt(anamnesis.getCompletedAt());
        e.setCreatedAt(anamnesis.getCreatedAt());
        e.setUpdatedAt(anamnesis.getUpdatedAt());
        return e;
    }

    private static PatientAnamnesis toDomain(PatientAnamnesisJpaEntity e) {
        return PatientAnamnesis.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getModelId(),
                e.getModelName(), e.getModelVersion(), e.getSnapshot(), e.getAnswers(), e.getAlerts(),
                PatientAnamnesisStatus.valueOf(e.getStatus()), e.getCreatedBy(), e.getCompletedBy(),
                e.getCompletedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
