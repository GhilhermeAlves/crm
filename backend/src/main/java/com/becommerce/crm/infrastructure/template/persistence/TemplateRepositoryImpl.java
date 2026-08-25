package com.becommerce.crm.infrastructure.template.persistence;

import com.becommerce.crm.application.template.port.output.TemplateRepository;
import com.becommerce.crm.domain.template.MessageTemplate;
import com.becommerce.crm.domain.template.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TemplateRepositoryImpl implements TemplateRepository {

    private final MessageTemplateJpaRepository jpaRepository;

    public TemplateRepositoryImpl(MessageTemplateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MessageTemplate save(MessageTemplate template) {
        return toDomain(jpaRepository.save(toEntity(template)));
    }

    @Override
    public Optional<MessageTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(TemplateRepositoryImpl::toDomain);
    }

    @Override
    public void delete(MessageTemplate template) {
        jpaRepository.deleteById(template.getId());
    }

    @Override
    public PageResult findByCompanyWithFilters(UUID companyId, String channelType, String status,
                                               int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Page<MessageTemplateJpaEntity> result = jpaRepository.findByCompanyWithFilters(
                companyId, channelType, status, PageRequest.of(page, pageSize, sort));
        List<MessageTemplate> content = result.getContent().stream()
                .map(TemplateRepositoryImpl::toDomain).toList();
        return new PageResult(content, result.getTotalElements());
    }

    private static MessageTemplateJpaEntity toEntity(MessageTemplate t) {
        MessageTemplateJpaEntity e = new MessageTemplateJpaEntity();
        e.setId(t.getId());
        e.setCompanyId(t.getCompanyId());
        e.setName(t.getName());
        e.setChannelType(t.getChannelType());
        e.setSubject(t.getSubject());
        e.setBody(t.getBody());
        e.setVariables(t.getVariables());
        e.setStatus(t.getStatus() != null ? t.getStatus().name() : TemplateStatus.ACTIVE.name());
        e.setVersion(t.getVersion());
        e.setExternalTemplateId(t.getExternalTemplateId());
        e.setCreatedAt(t.getCreatedAt());
        e.setUpdatedAt(t.getUpdatedAt());
        return e;
    }

    private static MessageTemplate toDomain(MessageTemplateJpaEntity e) {
        return MessageTemplate.reconstitute(
                e.getId(), e.getCompanyId(), e.getName(), e.getChannelType(), e.getSubject(),
                e.getBody(), e.getVariables(),
                e.getStatus() != null ? TemplateStatus.valueOf(e.getStatus()) : TemplateStatus.ACTIVE,
                e.getVersion(), e.getExternalTemplateId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
