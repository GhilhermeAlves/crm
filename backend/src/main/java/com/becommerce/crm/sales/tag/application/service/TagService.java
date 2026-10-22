package com.becommerce.crm.sales.tag.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.tag.application.dto.CreateTagRequest;
import com.becommerce.crm.sales.tag.application.dto.TagResponse;
import com.becommerce.crm.sales.tag.application.dto.UpdateTagRequest;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.sales.tag.application.port.out.TagRepository;
import com.becommerce.crm.sales.tag.domain.Tag;
import com.becommerce.crm.sales.tag.domain.exception.TagNotFoundException;
import com.becommerce.crm.sales.tag.domain.exception.TagValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TagService implements TagUseCase {

    private final TagRepository repository;
    private final TenantAuditRecorder auditor;

    public TagService(TagRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public TagResponse create(UUID companyId, CreateTagRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            String name = request.name() == null ? null : request.name().trim();
            repository.findByCompanyIdAndName(companyId, name)
                    .ifPresent(existing -> {
                        throw new TagValidationException("Já existe um marcador com o nome '" + name + "'.");
                    });
            Tag tag = Tag.create(companyId, name, request.color());
            repository.save(tag);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "Tag",
                    tag.getId().toString(), "Marcador criado: " + tag.getName(), null, null);
            return toResponse(tag);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getById(UUID companyId, UUID tagId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, tagId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public TagResponse update(UUID companyId, UUID tagId, UpdateTagRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Tag tag = requireOwned(companyId, tagId);
            if (request.name() != null && !request.name().isBlank()) {
                String name = request.name().trim();
                repository.findByCompanyIdAndName(companyId, name)
                        .filter(existing -> !existing.getId().equals(tagId))
                        .ifPresent(existing -> {
                            throw new TagValidationException("Já existe um marcador com o nome '" + name + "'.");
                        });
                tag.update(name, request.color());
        } else {
            if (request.color() == null) {
                tag.setColor(null);
            } else {
                tag.setColor(request.color());
            }
        }
        repository.save(tag);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Tag",
                    tag.getId().toString(), "Marcador atualizado: " + tag.getName(), null, null);
            return toResponse(tag);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID tagId) {
        try {
            TenantContext.setCompanyId(companyId);
            Tag tag = requireOwned(companyId, tagId);
            repository.delete(tag);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.SCHEDULING, "Tag",
                    tagId.toString(), "Marcador excluído: " + tag.getName(), null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyId(companyId).stream().map(TagService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInCompany(UUID companyId, UUID tagId) {
        try {
            TenantContext.setCompanyId(companyId);
            if (tagId == null) {
                return false;
            }
            return repository.findById(tagId)
                    .map(tag -> tag.getCompanyId().equals(companyId))
                    .orElse(false);
        } finally {
            TenantContext.clear();
        }
    }

    private Tag requireOwned(UUID companyId, UUID tagId) {
        Tag tag = repository.findById(tagId)
                .orElseThrow(() -> new TagNotFoundException("Marcador", tagId));
        if (!tag.getCompanyId().equals(companyId)) {
            throw new TagNotFoundException("Marcador", tagId);
        }
        return tag;
    }

    private static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getCompanyId(), tag.getName(), tag.getColor(),
                tag.getCreatedAt(), tag.getUpdatedAt());
    }
}
