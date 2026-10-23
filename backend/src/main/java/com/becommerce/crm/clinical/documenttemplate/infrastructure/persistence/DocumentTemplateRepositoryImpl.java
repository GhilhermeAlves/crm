package com.becommerce.crm.clinical.documenttemplate.infrastructure.persistence;

import com.becommerce.crm.clinical.documenttemplate.application.port.out.DocumentTemplateRepository;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentTemplate;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentTemplateRepositoryImpl implements DocumentTemplateRepository {

    private final DocumentTemplateJpaRepository jpaRepository;

    public DocumentTemplateRepositoryImpl(DocumentTemplateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DocumentTemplate save(DocumentTemplate template) {
        return toDomain(jpaRepository.save(toEntity(template)));
    }

    @Override
    public Optional<DocumentTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(DocumentTemplateRepositoryImpl::toDomain);
    }

    @Override
    public List<DocumentTemplate> findByCompanyIdOrderByTypeAscTitleAsc(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByTypeAscTitleAsc(companyId).stream()
                .map(DocumentTemplateRepositoryImpl::toDomain).toList();
    }

    @Override
    public long countByCompanyId(UUID companyId) {
        return jpaRepository.countByCompanyId(companyId);
    }

    @Override
    public void delete(DocumentTemplate template) {
        jpaRepository.deleteById(template.getId());
    }

    private static DocumentTemplateJpaEntity toEntity(DocumentTemplate template) {
        DocumentTemplateJpaEntity e = new DocumentTemplateJpaEntity();
        e.setId(template.getId());
        e.setCompanyId(template.getCompanyId());
        e.setType(template.getType().name());
        e.setTitle(template.getTitle());
        e.setBody(template.getBody());
        e.setActive(template.isActive());
        e.setCreatedBy(template.getCreatedBy());
        e.setCreatedAt(template.getCreatedAt());
        e.setUpdatedAt(template.getUpdatedAt());
        return e;
    }

    private static DocumentTemplate toDomain(DocumentTemplateJpaEntity e) {
        return DocumentTemplate.reconstitute(e.getId(), e.getCompanyId(), DocumentType.valueOf(e.getType()),
                e.getTitle(), e.getBody(), e.isActive(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
