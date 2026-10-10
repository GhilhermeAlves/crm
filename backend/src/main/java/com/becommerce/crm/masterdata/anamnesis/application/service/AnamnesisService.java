package com.becommerce.crm.masterdata.anamnesis.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelSummaryResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.CreateAnamnesisModelRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.UpdateAnamnesisModelRequest;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisModelRepository;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisQuestionOptionRepository;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisQuestionRepository;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisSectionRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisComplementTrigger;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisDefaultTemplate;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisModel;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestion;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionOption;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisSection;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisModelNotFoundException;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modelos de anamnese editáveis por empresa (V090). Isolamento por
 * {@code TenantContext} + RLS FORCE. A primeira consulta de uma empresa
 * provisiona, de forma idempotente, a "Anamnese Odontológica Padrão" — que a
 * partir daí é totalmente editável e não afeta outras empresas.
 */
@Service
public class AnamnesisService {

    private static final String ENTITY = "AnamnesisModel";
    private static final String DEFAULT_COMPLEMENT_LABEL = "Descreva";

    private final AnamnesisModelRepository modelRepository;
    private final AnamnesisSectionRepository sectionRepository;
    private final AnamnesisQuestionRepository questionRepository;
    private final AnamnesisQuestionOptionRepository optionRepository;
    private final TenantAuditRecorder auditor;

    public AnamnesisService(AnamnesisModelRepository modelRepository,
                            AnamnesisSectionRepository sectionRepository,
                            AnamnesisQuestionRepository questionRepository,
                            AnamnesisQuestionOptionRepository optionRepository,
                            TenantAuditRecorder auditor) {
        this.modelRepository = modelRepository;
        this.sectionRepository = sectionRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.auditor = auditor;
    }

    @Transactional
    public List<AnamnesisModelSummaryResponse> list(UUID companyId, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            ensureDefaultTemplate(companyId, createdBy);
            List<AnamnesisModel> models = modelRepository.findByCompanyIdOrderByName(companyId);
            List<AnamnesisModelSummaryResponse> result = new ArrayList<>();
            for (AnamnesisModel model : models) {
                List<AnamnesisSection> sections = sectionRepository.findByModelIdOrderBySortOrder(model.getId());
                List<AnamnesisQuestion> questions =
                        questionRepository.findByModelIdOrderBySortOrder(model.getId());
                result.add(new AnamnesisModelSummaryResponse(model.getId(), model.getCompanyId(),
                        model.getName(), model.getDescription(), model.isActive(), model.isDefault(),
                        model.getVersion(), sections.size(), questions.size(), model.getCreatedAt(),
                        model.getUpdatedAt()));
            }
            return result;
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public AnamnesisModelResponse getById(UUID companyId, UUID modelId) {
        try {
            TenantContext.setCompanyId(companyId);
            AnamnesisModel model = requireOwned(companyId, modelId);
            return toResponse(model);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public AnamnesisModelResponse create(UUID companyId, CreateAnamnesisModelRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            AnamnesisModel model = AnamnesisModel.create(UUID.randomUUID(), companyId, request.name(),
                    request.description(), createdBy);
            AnamnesisModel saved = modelRepository.save(model);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.ANAMNESIS, ENTITY,
                    saved.getId().toString(), "Modelo de anamnese criado: " + saved.getName(), createdBy, null);
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public AnamnesisModelResponse update(UUID companyId, UUID modelId, UpdateAnamnesisModelRequest request,
                                         UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            AnamnesisModel model = requireOwned(companyId, modelId);
            model.update(request.name(), request.description());
            AnamnesisModel saved = modelRepository.save(model);
            replaceStructure(saved.getId(), companyId, request);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.ANAMNESIS, ENTITY,
                    saved.getId().toString(), "Modelo de anamnese atualizado: " + saved.getName(), createdBy,
                    Map.of("version", String.valueOf(saved.getVersion())));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public AnamnesisModelResponse setActive(UUID companyId, UUID modelId, boolean active, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            AnamnesisModel model = requireOwned(companyId, modelId);
            if (active) {
                model.activate();
            } else {
                model.deactivate();
            }
            AnamnesisModel saved = modelRepository.save(model);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.ANAMNESIS, ENTITY,
                    saved.getId().toString(),
                    (active ? "Modelo de anamnese ativado: " : "Modelo de anamnese desativado: ")
                            + saved.getName(), createdBy, Map.of("active", String.valueOf(active)));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void delete(UUID companyId, UUID modelId, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            AnamnesisModel model = requireOwned(companyId, modelId);
            if (model.isDefault()) {
                throw new AnamnesisValidationException(
                        "O modelo padrão não pode ser excluído. Desative-o ou edite-o.");
            }
            questionRepository.deleteByModelId(modelId);
            sectionRepository.deleteByModelId(modelId);
            modelRepository.delete(model);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.ANAMNESIS, ENTITY,
                    modelId.toString(), "Modelo de anamnese excluído: " + model.getName(), createdBy, null);
        } finally {
            TenantContext.clear();
        }
    }

    // ------------------------------------------------------------------
    // Provisionamento do modelo padrão
    // ------------------------------------------------------------------
    private void ensureDefaultTemplate(UUID companyId, UUID createdBy) {
        if (modelRepository.existsByCompanyId(companyId)) {
            return;
        }
        try {
            persistDraft(AnamnesisDefaultTemplate.build(companyId, createdBy));
        } catch (DataIntegrityViolationException raceLost) {
            // Outra requisição provisionou primeiro (índice único parcial de is_default).
        }
    }

    private void persistDraft(com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisTemplateDraft draft) {
        modelRepository.save(draft.model());
        for (com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisTemplateDraft.SectionDraft section
                : draft.sections()) {
            sectionRepository.save(section.section());
            for (com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisTemplateDraft.QuestionDraft question
                    : section.questions()) {
                questionRepository.save(question.question());
                for (AnamnesisQuestionOption option : question.options()) {
                    optionRepository.save(option);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Substituição da estrutura (update)
    // ------------------------------------------------------------------
    private void replaceStructure(UUID modelId, UUID companyId, UpdateAnamnesisModelRequest request) {
        questionRepository.deleteByModelId(modelId);
        sectionRepository.deleteByModelId(modelId);

        List<com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionRequest> sections =
                request.sections() == null ? List.of() : request.sections();
        int sectionOrder = 0;
        for (var sectionReq : sections) {
            UUID sectionId = orRandom(sectionReq.id());
            AnamnesisSection section = AnamnesisSection.create(sectionId, modelId, companyId,
                    sectionReq.title(), Boolean.TRUE.equals(sectionReq.professional()), sectionOrder++);
            sectionRepository.save(section);

            List<com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionRequest> questions =
                    sectionReq.questions() == null ? List.of() : sectionReq.questions();
            int questionOrder = 0;
            for (var questionReq : questions) {
                UUID questionId = orRandom(questionReq.id());
                AnamnesisQuestion question = buildQuestion(companyId, modelId, sectionId, questionId,
                        questionReq, questionOrder++);
                questionRepository.save(question);
                persistOptions(companyId, question, questionReq);
            }
        }
    }

    private AnamnesisQuestion buildQuestion(UUID companyId, UUID modelId, UUID sectionId, UUID questionId,
                                             com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionRequest req,
                                             int sortOrder) {
        AnamnesisQuestionType type = req.type();
        boolean allowComplement = Boolean.TRUE.equals(req.allowComplement())
                || type == AnamnesisQuestionType.YES_NO_WITH_TEXT;
        AnamnesisComplementTrigger trigger = AnamnesisComplementTrigger.YES;
        String complementLabel = null;
        String optionValue = null;
        if (allowComplement) {
            trigger = req.complementTrigger() == null ? AnamnesisComplementTrigger.YES : req.complementTrigger();
            complementLabel = req.complementLabel();
            if (complementLabel == null || complementLabel.isBlank()) {
                complementLabel = DEFAULT_COMPLEMENT_LABEL;
            }
            if (trigger == AnamnesisComplementTrigger.OPTION) {
                String candidate = req.complementOptionValue();
                List<com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionRequest> options =
                        req.options() == null ? List.of() : req.options();
                boolean matches = options.stream().anyMatch(o -> o.value() != null
                        ? o.value().equals(candidate) : o.label().equals(candidate));
                if (!matches) {
                    throw new AnamnesisValidationException(
                            "A opção que dispara o campo complementar deve existir entre as opções da pergunta.");
                }
                optionValue = candidate;
            }
        }
        return AnamnesisQuestion.create(questionId, sectionId, modelId, companyId, req.text(), type,
                Boolean.TRUE.equals(req.required()), Boolean.TRUE.equals(req.highlight()), allowComplement,
                complementLabel, trigger, optionValue, sortOrder);
    }

    private void persistOptions(UUID companyId, AnamnesisQuestion question,
                                com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionRequest req) {
        AnamnesisQuestionType type = question.getType();
        boolean selectable = type == AnamnesisQuestionType.SINGLE_SELECT
                || type == AnamnesisQuestionType.MULTI_SELECT;
        List<com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionRequest> options =
                req.options() == null ? List.of() : req.options();
        if (!selectable) {
            return;
        }
        if (options.isEmpty()) {
            throw new AnamnesisValidationException(
                    "Perguntas de seleção precisam de pelo menos uma opção de resposta.");
        }
        int order = 0;
        for (var option : options) {
            optionRepository.save(AnamnesisQuestionOption.create(orRandom(option.id()), question.getId(),
                    companyId, option.label(), option.value(), order++));
        }
    }

    // ------------------------------------------------------------------
    // Montagem da resposta
    // ------------------------------------------------------------------
    private AnamnesisModelResponse toResponse(AnamnesisModel model) {
        UUID modelId = model.getId();
        List<AnamnesisSection> sections = sectionRepository.findByModelIdOrderBySortOrder(modelId);
        List<AnamnesisQuestion> questions = questionRepository.findByModelIdOrderBySortOrder(modelId);
        List<AnamnesisQuestionOption> options = optionRepository.findByModelIdOrderBySortOrder(modelId);

        Map<UUID, List<AnamnesisQuestion>> questionsBySection = new LinkedHashMap<>();
        for (AnamnesisQuestion question : questions) {
            questionsBySection.computeIfAbsent(question.getSectionId(), k -> new ArrayList<>()).add(question);
        }
        Map<UUID, List<AnamnesisQuestionOption>> optionsByQuestion = new LinkedHashMap<>();
        for (AnamnesisQuestionOption option : options) {
            optionsByQuestion.computeIfAbsent(option.getQuestionId(), k -> new ArrayList<>()).add(option);
        }

        List<AnamnesisSectionResponse> sectionResponses = new ArrayList<>();
        for (AnamnesisSection section : sections) {
            List<AnamnesisQuestionResponse> questionResponses = new ArrayList<>();
            for (AnamnesisQuestion question : questionsBySection.getOrDefault(section.getId(), List.of())) {
                List<AnamnesisOptionResponse> optionResponses =
                        optionsByQuestion.getOrDefault(question.getId(), List.of()).stream()
                                .map(AnamnesisOptionResponse::from).toList();
                questionResponses.add(AnamnesisQuestionResponse.from(question, optionResponses));
            }
            sectionResponses.add(AnamnesisSectionResponse.from(section, questionResponses));
        }
        return new AnamnesisModelResponse(model.getId(), model.getCompanyId(), model.getName(),
                model.getDescription(), model.isActive(), model.isDefault(), model.getVersion(),
                sectionResponses, model.getCreatedAt(), model.getUpdatedAt());
    }

    private AnamnesisModel requireOwned(UUID companyId, UUID modelId) {
        AnamnesisModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new AnamnesisModelNotFoundException(modelId));
        if (!model.getCompanyId().equals(companyId)) {
            throw new AnamnesisModelNotFoundException(modelId);
        }
        return model;
    }

    private static UUID orRandom(UUID id) {
        return id != null ? id : UUID.randomUUID();
    }
}
