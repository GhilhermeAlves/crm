package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisResponse;
import com.becommerce.crm.clinical.anamnesis.application.dto.SavePatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.dto.StartPatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.port.in.PatientAnamnesisUseCase;
import com.becommerce.crm.clinical.anamnesis.application.port.out.PatientAnamnesisRepository;
import com.becommerce.crm.clinical.anamnesis.domain.AnamnesisAlertExtractor;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesis;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;
import com.becommerce.crm.clinical.anamnesis.domain.exception.PatientAnamnesisNotFoundException;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.service.AnamnesisService;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anamnese preenchida do paciente (ficha do paciente, Fase 2). Guarda um
 * snapshot do modelo no início; a conclusão valida perguntas obrigatórias,
 * calcula alertas e torna a anamnese imutável. Cada operação isola a empresa
 * ativa no {@link TenantContext}; o contato e a anamnese precisam pertencer à
 * mesma empresa (defense-in-depth além do RLS). Rascunhos só podem ser
 * alterados pelo autor ou por administradores.
 */
@Service
public class PatientAnamnesisService implements PatientAnamnesisUseCase {

    private static final String RESOURCE = "PatientAnamnesis";

    private final PatientAnamnesisRepository anamnesisRepository;
    private final ContactRepository contactRepository;
    private final AnamnesisService modelService;
    private final UserRepository userRepository;
    private final TenantAuditRecorder auditor;
    private final ObjectMapper objectMapper;

    public PatientAnamnesisService(PatientAnamnesisRepository anamnesisRepository,
                                   ContactRepository contactRepository,
                                   AnamnesisService modelService,
                                   UserRepository userRepository,
                                   TenantAuditRecorder auditor,
                                   ObjectMapper objectMapper) {
        this.anamnesisRepository = anamnesisRepository;
        this.contactRepository = contactRepository;
        this.modelService = modelService;
        this.userRepository = userRepository;
        this.auditor = auditor;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientAnamnesisResponse> list(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Map<UUID, String> authorNames = new HashMap<>();
            return anamnesisRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId)
                    .stream()
                    .map(anamnesis -> toResponse(anamnesis, authorNames))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<PatientAnamnesisResponse> current(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            return anamnesisRepository.findFirstByCompanyIdAndContactIdAndStatusOrderByCompletedAtDesc(
                            companyId, contactId, PatientAnamnesisStatus.COMPLETED)
                    .map(anamnesis -> toResponse(anamnesis, new HashMap<>()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PatientAnamnesisResponse get(UUID companyId, UUID contactId, UUID anamnesisId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            return toResponse(requireOwned(companyId, contactId, anamnesisId), new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientAnamnesisResponse start(UUID companyId, UUID contactId,
                                          StartPatientAnamnesisRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            AnamnesisModelResponse model = request != null && request.modelId() != null
                    ? modelService.getById(companyId, request.modelId())
                    : modelService.getDefaultModel(companyId, createdBy);
            if (!model.active()) {
                throw new ContactValidationException("Este modelo de anamnese está inativo.");
            }
            PatientAnamnesis anamnesis = PatientAnamnesis.create(companyId, contactId, model.id(),
                    model.name(), model.version(), writeJson(model), createdBy);
            anamnesisRepository.save(anamnesis);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, RESOURCE,
                    anamnesis.getId().toString(), "Anamnese iniciada para o paciente.", createdBy, null);
            return toResponse(anamnesis, new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientAnamnesisResponse save(UUID companyId, UUID contactId, UUID anamnesisId,
                                         SavePatientAnamnesisRequest request, UUID actorId, boolean admin) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientAnamnesis anamnesis = requireOwned(companyId, contactId, anamnesisId);
            assertCanModify(anamnesis, actorId, admin);
            if (!anamnesis.isDraft()) {
                throw new ContactValidationException(
                        "Anamnese concluída não pode ser alterada. Inicie uma nova.");
            }

            AnamnesisModelResponse snapshot = readSnapshot(anamnesis);
            Map<UUID, PatientAnamnesisAnswer> answers = filterAnswers(snapshot, request.answers());
            boolean complete = request.complete();
            if (complete) {
                requireRequiredAnswers(snapshot, answers);
                List<String> alerts = AnamnesisAlertExtractor.extract(snapshot, answers);
                anamnesis.complete(writeJson(answers), alerts, actorId);
            } else {
                anamnesis.saveDraft(writeJson(answers));
            }
            anamnesisRepository.save(anamnesis);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    anamnesis.getId().toString(),
                    complete ? "Anamnese concluída." : "Anamnese salva como rascunho.", actorId, null);
            return toResponse(anamnesis, new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID anamnesisId, UUID actorId, boolean admin) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientAnamnesis anamnesis = requireOwned(companyId, contactId, anamnesisId);
            assertCanModify(anamnesis, actorId, admin);
            if (!anamnesis.isDraft()) {
                throw new ContactValidationException(
                        "Anamnese concluída não pode ser excluída.");
            }
            anamnesisRepository.delete(anamnesis);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, RESOURCE,
                    anamnesisId.toString(), "Anamnese excluída do paciente.", actorId, null);
        } finally {
            TenantContext.clear();
        }
    }

    private static void assertCanModify(PatientAnamnesis anamnesis, UUID actorId, boolean admin) {
        if (!admin && !anamnesis.isAuthoredBy(actorId)) {
            throw new CrmAccessDeniedException(
                    "Você só pode editar ou excluir as anamneses criadas por você.");
        }
    }

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private PatientAnamnesis requireOwned(UUID companyId, UUID contactId, UUID anamnesisId) {
        PatientAnamnesis anamnesis = anamnesisRepository.findById(anamnesisId)
                .orElseThrow(() -> new PatientAnamnesisNotFoundException(anamnesisId));
        if (!anamnesis.getCompanyId().equals(companyId) || !anamnesis.getContactId().equals(contactId)) {
            throw new PatientAnamnesisNotFoundException(anamnesisId);
        }
        return anamnesis;
    }

    private AnamnesisModelResponse readSnapshot(PatientAnamnesis anamnesis) {
        try {
            return objectMapper.readValue(anamnesis.getSnapshot(), AnamnesisModelResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Snapshot de anamnese inválido.", ex);
        }
    }

    private Map<UUID, PatientAnamnesisAnswer> filterAnswers(AnamnesisModelResponse snapshot,
                                                             Map<UUID, PatientAnamnesisAnswer> answers) {
        Map<UUID, PatientAnamnesisAnswer> filtered = new LinkedHashMap<>();
        if (snapshot.sections() == null || answers == null) {
            return filtered;
        }
        for (AnamnesisSectionResponse section : snapshot.sections()) {
            if (section.questions() == null) {
                continue;
            }
            for (AnamnesisQuestionResponse question : section.questions()) {
                if (answers.containsKey(question.id())) {
                    filtered.put(question.id(), answers.get(question.id()));
                }
            }
        }
        return filtered;
    }

    private static void requireRequiredAnswers(AnamnesisModelResponse snapshot,
                                               Map<UUID, PatientAnamnesisAnswer> answers) {
        List<String> missing = new ArrayList<>();
        if (snapshot.sections() != null) {
            for (AnamnesisSectionResponse section : snapshot.sections()) {
                if (section.questions() == null) {
                    continue;
                }
                for (AnamnesisQuestionResponse question : section.questions()) {
                    if (question.required() && !hasAnswer(answers.get(question.id()))) {
                        missing.add(question.text());
                    }
                }
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        String listed = String.join("; ", missing.stream().limit(3).toList());
        if (missing.size() > 3) {
            listed += " (+" + (missing.size() - 3) + ")";
        }
        throw new ContactValidationException("Responda as perguntas obrigatórias: " + listed);
    }

    private static boolean hasAnswer(PatientAnamnesisAnswer answer) {
        if (answer == null || answer.value() == null) {
            return false;
        }
        Object value = answer.value();
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar anamnese.", ex);
        }
    }

    private Map<UUID, PatientAnamnesisAnswer> readAnswers(PatientAnamnesis anamnesis) {
        try {
            return objectMapper.readValue(anamnesis.getAnswers(),
                    new TypeReference<LinkedHashMap<UUID, PatientAnamnesisAnswer>>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Respostas de anamnese inválidas.", ex);
        }
    }

    private PatientAnamnesisResponse toResponse(PatientAnamnesis anamnesis, Map<UUID, String> cache) {
        return new PatientAnamnesisResponse(
                anamnesis.getId(),
                anamnesis.getContactId(),
                anamnesis.getModelId(),
                anamnesis.getModelName(),
                anamnesis.getModelVersion(),
                readSnapshot(anamnesis),
                readAnswers(anamnesis),
                anamnesis.getAlerts(),
                anamnesis.getStatus(),
                anamnesis.getCreatedBy(),
                userName(anamnesis.getCreatedBy(), cache),
                anamnesis.getCompletedBy(),
                userName(anamnesis.getCompletedBy(), cache),
                anamnesis.getCompletedAt(),
                anamnesis.getCreatedAt(),
                anamnesis.getUpdatedAt());
    }

    private String userName(UUID userId, Map<UUID, String> cache) {
        if (userId == null) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        String name = userRepository.findById(userId).map(User::getName).orElse(null);
        cache.put(userId, name);
        return name;
    }
}
