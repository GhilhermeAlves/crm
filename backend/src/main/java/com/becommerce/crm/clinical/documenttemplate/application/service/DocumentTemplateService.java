package com.becommerce.crm.clinical.documenttemplate.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateRequest;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateResponse;
import com.becommerce.crm.clinical.documenttemplate.application.port.in.DocumentTemplateUseCase;
import com.becommerce.crm.clinical.documenttemplate.application.port.out.DocumentTemplateRepository;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentTemplate;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import com.becommerce.crm.clinical.documenttemplate.domain.exception.DocumentTemplateNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Modelos de documento por empresa (Configurações -> Documentos, Fase 2 da
 * ficha). Isolamento via {@link TenantContext} + RLS FORCE. A primeira listagem
 * de uma empresa sem modelos semeia três modelos padrão de forma idempotente,
 * no mesmo espírito do modelo padrão de anamnese.
 */
@Service
public class DocumentTemplateService implements DocumentTemplateUseCase {

    private static final String RESOURCE = "DocumentTemplate";

    private static final String CERTIFICATE_ATTENDANCE_BODY =
            "Atesto, para os devidos fins, que {{paciente.nome}}, CPF {{paciente.cpf}}, "
                    + "esteve em atendimento odontológico nesta clínica em {{data}}, "
                    + "no período das ____ às ____.";

    private static final String CERTIFICATE_LEAVE_BODY =
            "Atesto, para os devidos fins, que {{paciente.nome}}, CPF {{paciente.cpf}}, "
                    + "necessita de ____ dia(s) de afastamento de suas atividades a partir de "
                    + "{{data}}, por motivo de tratamento odontológico.";

    private static final String PRESCRIPTION_BODY =
            "Paciente: {{paciente.nome}}\n\nUso oral:\n\n"
                    + "1. ________________________________\n"
                    + "   Tomar ____________________________\n\n"
                    + "{{cidade}}, {{data}}.";

    private final DocumentTemplateRepository templateRepository;
    private final TenantAuditRecorder auditor;

    public DocumentTemplateService(DocumentTemplateRepository templateRepository,
                                   TenantAuditRecorder auditor) {
        this.templateRepository = templateRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public List<DocumentTemplateResponse> list(UUID companyId, DocumentType type, boolean activeOnly) {
        try {
            TenantContext.setCompanyId(companyId);
            ensureDefaultTemplates(companyId);
            return templateRepository.findByCompanyIdOrderByTypeAscTitleAsc(companyId).stream()
                    .filter(template -> type == null || template.getType() == type)
                    .filter(template -> !activeOnly || template.isActive())
                    .map(DocumentTemplateService::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentTemplateResponse get(UUID companyId, UUID templateId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, templateId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public DocumentTemplateResponse create(UUID companyId, DocumentTemplateRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            DocumentTemplate template = DocumentTemplate.create(companyId, request.type(), request.title(),
                    request.body(), createdBy);
            if (request.active() != null && !request.active()) {
                template.edit(template.getType(), template.getTitle(), template.getBody(), false);
            }
            DocumentTemplate saved = templateRepository.save(template);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.SETTINGS, RESOURCE,
                    saved.getId().toString(), "Modelo de documento criado: " + saved.getTitle(), createdBy, null);
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public DocumentTemplateResponse update(UUID companyId, UUID templateId, DocumentTemplateRequest request,
                                           UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            DocumentTemplate template = requireOwned(companyId, templateId);
            template.edit(request.type(), request.title(), request.body(), request.active());
            DocumentTemplate saved = templateRepository.save(template);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, RESOURCE,
                    saved.getId().toString(), "Modelo de documento atualizado: " + saved.getTitle(), createdBy, null);
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID templateId, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            DocumentTemplate template = requireOwned(companyId, templateId);
            templateRepository.delete(template);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.SETTINGS, RESOURCE,
                    templateId.toString(), "Modelo de documento excluído: " + template.getTitle(), createdBy, null);
        } finally {
            TenantContext.clear();
        }
    }

    private void ensureDefaultTemplates(UUID companyId) {
        if (templateRepository.countByCompanyId(companyId) > 0) {
            return;
        }
        try {
            createDefault(companyId, DocumentType.CERTIFICATE, "Atestado de comparecimento",
                    CERTIFICATE_ATTENDANCE_BODY);
            createDefault(companyId, DocumentType.CERTIFICATE, "Atestado de afastamento",
                    CERTIFICATE_LEAVE_BODY);
            createDefault(companyId, DocumentType.PRESCRIPTION, "Receituário simples", PRESCRIPTION_BODY);
        } catch (DataIntegrityViolationException raceLost) {
            // Outra requisição semeou primeiro; a lista abaixo reflete o que ficou.
        }
    }

    private void createDefault(UUID companyId, DocumentType type, String title, String body) {
        templateRepository.save(DocumentTemplate.create(companyId, type, title, body, null));
    }

    private DocumentTemplate requireOwned(UUID companyId, UUID templateId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new DocumentTemplateNotFoundException(templateId));
        if (!template.getCompanyId().equals(companyId)) {
            throw new DocumentTemplateNotFoundException(templateId);
        }
        return template;
    }

    private static DocumentTemplateResponse toResponse(DocumentTemplate template) {
        return new DocumentTemplateResponse(template.getId(), template.getType(), template.getTitle(),
                template.getBody(), template.isActive(), template.getCreatedAt(), template.getUpdatedAt());
    }
}
