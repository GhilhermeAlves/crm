package com.becommerce.crm.clinical.evolution.infrastructure.persistence;

import com.becommerce.crm.clinical.evolution.application.port.out.PatientEvolutionRepository;
import com.becommerce.crm.clinical.evolution.domain.PatientEvolution;
import com.becommerce.crm.clinical.evolution.domain.PatientEvolutionStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PatientEvolutionRepositoryImpl implements PatientEvolutionRepository {

    private final PatientEvolutionJpaRepository jpaRepository;

    public PatientEvolutionRepositoryImpl(PatientEvolutionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PatientEvolution save(PatientEvolution evolution) {
        return toDomain(jpaRepository.save(toEntity(evolution)));
    }

    @Override
    public Optional<PatientEvolution> findById(UUID id) {
        return jpaRepository.findById(id).map(PatientEvolutionRepositoryImpl::toDomain);
    }

    @Override
    public List<PatientEvolution> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId,
                                                                                  UUID contactId) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId).stream()
                .map(PatientEvolutionRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(PatientEvolution evolution) {
        jpaRepository.deleteById(evolution.getId());
    }

    private static PatientEvolutionJpaEntity toEntity(PatientEvolution evolution) {
        PatientEvolutionJpaEntity e = new PatientEvolutionJpaEntity();
        e.setId(evolution.getId());
        e.setCompanyId(evolution.getCompanyId());
        e.setContactId(evolution.getContactId());
        e.setAppointmentId(evolution.getAppointmentId());
        e.setBody(evolution.getBody());
        e.setStatus(evolution.getStatus().name());
        e.setCreatedBy(evolution.getCreatedBy());
        e.setFinalizedBy(evolution.getFinalizedBy());
        e.setFinalizedAt(evolution.getFinalizedAt());
        e.setCreatedAt(evolution.getCreatedAt());
        e.setUpdatedAt(evolution.getUpdatedAt());
        return e;
    }

    private static PatientEvolution toDomain(PatientEvolutionJpaEntity e) {
        return PatientEvolution.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(),
                e.getAppointmentId(), e.getBody(), PatientEvolutionStatus.valueOf(e.getStatus()),
                e.getCreatedBy(), e.getFinalizedBy(), e.getFinalizedAt(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
