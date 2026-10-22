package com.becommerce.crm.clinical.document.infrastructure.persistence;

import com.becommerce.crm.clinical.document.application.port.out.PatientDocumentRepository;
import com.becommerce.crm.clinical.document.domain.PatientDocument;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PatientDocumentRepositoryImpl implements PatientDocumentRepository {

    private final PatientDocumentJpaRepository jpaRepository;

    public PatientDocumentRepositoryImpl(PatientDocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PatientDocument save(PatientDocument document) {
        return toDomain(jpaRepository.save(toEntity(document)));
    }

    @Override
    public Optional<PatientDocument> findById(UUID id) {
        return jpaRepository.findById(id).map(PatientDocumentRepositoryImpl::toDomain);
    }

    @Override
    public List<PatientDocument> findByCompanyIdAndContactIdOrderByIssuedAtDesc(UUID companyId,
                                                                                UUID contactId) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByIssuedAtDesc(companyId, contactId).stream()
                .map(PatientDocumentRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(PatientDocument document) {
        jpaRepository.deleteById(document.getId());
    }

    private static PatientDocumentJpaEntity toEntity(PatientDocument document) {
        PatientDocumentJpaEntity e = new PatientDocumentJpaEntity();
        e.setId(document.getId());
        e.setCompanyId(document.getCompanyId());
        e.setContactId(document.getContactId());
        e.setTemplateId(document.getTemplateId());
        e.setType(document.getType().name());
        e.setTitle(document.getTitle());
        e.setBody(document.getBody());
        e.setProfessionalName(document.getProfessionalName());
        e.setProfessionalRegistry(document.getProfessionalRegistry());
        e.setIssuedBy(document.getIssuedBy());
        e.setIssuedAt(document.getIssuedAt());
        return e;
    }

    private static PatientDocument toDomain(PatientDocumentJpaEntity e) {
        return PatientDocument.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(),
                e.getTemplateId(), DocumentType.valueOf(e.getType()), e.getTitle(), e.getBody(),
                e.getProfessionalName(), e.getProfessionalRegistry(), e.getIssuedBy(), e.getIssuedAt());
    }
}
