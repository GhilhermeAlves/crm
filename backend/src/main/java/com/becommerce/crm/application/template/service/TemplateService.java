package com.becommerce.crm.application.template.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.template.dto.CreateTemplateRequest;
import com.becommerce.crm.application.template.dto.TemplateResponse;
import com.becommerce.crm.application.template.dto.UpdateTemplateRequest;
import com.becommerce.crm.application.template.port.input.TemplateUseCase;
import com.becommerce.crm.application.template.port.output.TemplateRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.template.MessageTemplate;
import com.becommerce.crm.domain.template.exception.TemplateNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Templates de mensagem (Sprint 17). Isolamento por {@code TenantContext} +
 * RLS FORCE (V055); edição incrementa a versão (versionamento por execução).
 */
@Service
public class TemplateService implements TemplateUseCase {

    private final TemplateRepository templateRepository;
    private final TenantAuditRecorder auditor;

    public TemplateService(TemplateRepository templateRepository, TenantAuditRecorder auditor) {
        this.templateRepository = templateRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public TemplateResponse create(UUID companyId, CreateTemplateRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            MessageTemplate template = MessageTemplate.create(companyId, request.name(),
                    request.channelType(), request.subject(), request.body(), request.variables(),
                    request.externalTemplateId());
            MessageTemplate saved = templateRepository.save(template);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.TEMPLATES, "MessageTemplate",
                    saved.getId().toString(), "Template criado: " + saved.getName(),
                    null, java.util.Map.of("channelType", saved.getChannelType()));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getById(UUID companyId, UUID templateId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, templateId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public TemplateResponse update(UUID companyId, UUID templateId, UpdateTemplateRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            MessageTemplate template = requireOwned(companyId, templateId);
            if (request.name() != null) {
                template.rename(request.name());
            }
            template.updateContent(request.subject(), request.body(), request.variables());
            MessageTemplate saved = templateRepository.save(template);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.TEMPLATES, "MessageTemplate",
                    saved.getId().toString(), "Template atualizado: versão " + saved.getVersion(),
                    null, java.util.Map.of("version", String.valueOf(saved.getVersion())));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID templateId) {
        try {
            TenantContext.setCompanyId(companyId);
            MessageTemplate template = requireOwned(companyId, templateId);
            template.archive();
            templateRepository.save(template);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.TEMPLATES, "MessageTemplate",
                    templateId.toString(), "Template arquivado", null,
                    java.util.Map.of("name", template.getName()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TemplateResponse> list(UUID companyId, String channelType, String status,
                                               int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            var result = templateRepository.findByCompanyWithFilters(
                    companyId, normalize(channelType), normalize(status), page, pageSize);
            var content = result.content().stream().map(TemplateService::toResponse).toList();
            return PageResponse.of(content, page, pageSize, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    private MessageTemplate requireOwned(UUID companyId, UUID templateId) {
        MessageTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        if (!template.getCompanyId().equals(companyId)) {
            throw new TemplateNotFoundException(templateId);
        }
        return template;
    }

    private String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return com.becommerce.crm.domain.template.TemplateStatus.valueOf(raw).name();
        } catch (IllegalArgumentException ignored) {
            // não é status; devolve como veio (ex.: channel_type)
        }
        return raw;
    }

    private static TemplateResponse toResponse(MessageTemplate t) {
        return new TemplateResponse(t.getId(), t.getCompanyId(), t.getName(), t.getChannelType(),
                t.getSubject(), t.getBody(), t.extractVariables(), t.getStatus(), t.getVersion(),
                t.getExternalTemplateId(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
