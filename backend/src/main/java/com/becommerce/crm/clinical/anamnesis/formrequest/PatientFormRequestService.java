package com.becommerce.crm.clinical.anamnesis.formrequest;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisResponse;
import com.becommerce.crm.clinical.anamnesis.application.dto.SavePatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.dto.StartPatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.port.in.PatientAnamnesisUseCase;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.CreateFormRequest;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.CreatedFormRequest;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.FormRequestResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.PublicFormResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.SubmitPublicForm;
import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppConnectionService;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNoLongerValidException;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNotFoundException;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Formulário (anamnese) respondido pelo paciente, por link ou no modo recepção.
 *
 * <p>No envio é criada uma anamnese em rascunho e um pedido com token. A rota
 * pública segue o padrão do link de cadastro: o hash do token vai para o GUC
 * transacional ({@link FormTokenContextHolder}), a policy RLS libera só aquele
 * pedido, e a empresa/paciente vêm da própria linha para fixar o
 * {@link TenantContext}. Expiração e uso são checados aqui.
 *
 * <p>Seções "do profissional" não aparecem para o paciente; se o modelo tiver
 * alguma, a anamnese fica em rascunho com as respostas do paciente para o
 * profissional completar e concluir.
 */
@Service
public class PatientFormRequestService {

    static final String MODE_LINK = "LINK";
    static final String MODE_RECEPTION = "RECEPTION";
    static final int LINK_DAYS = 7;
    static final int RECEPTION_HOURS = 12;
    static final int MAX_SIGNATURE_BYTES = 500 * 1024;
    private static final String PNG_PREFIX = "data:image/png;base64,";

    private static final Logger log = LoggerFactory.getLogger(PatientFormRequestService.class);

    private final PatientFormRequestJpaRepository repository;
    private final FormTokenContextHolder tokenContext;
    private final PatientAnamnesisUseCase anamnesisUseCase;
    private final ContactUseCase contactUseCase;
    private final CompanyRepository companyRepository;
    private final StorageUseCase storageUseCase;
    private final NotificationUseCase notifications;
    private final PatientTimelineRecorder timeline;
    private final ObjectMapper objectMapper;
    private final String publicBaseUrl;

    public PatientFormRequestService(PatientFormRequestJpaRepository repository,
                                     FormTokenContextHolder tokenContext,
                                     PatientAnamnesisUseCase anamnesisUseCase,
                                     ContactUseCase contactUseCase,
                                     CompanyRepository companyRepository,
                                     StorageUseCase storageUseCase,
                                     NotificationUseCase notifications,
                                     PatientTimelineRecorder timeline,
                                     ObjectMapper objectMapper,
                                     @Value("${app.mail.frontend-base-url:}") String publicBaseUrl) {
        this.repository = repository;
        this.tokenContext = tokenContext;
        this.anamnesisUseCase = anamnesisUseCase;
        this.contactUseCase = contactUseCase;
        this.companyRepository = companyRepository;
        this.storageUseCase = storageUseCase;
        this.notifications = notifications;
        this.timeline = timeline;
        this.objectMapper = objectMapper;
        this.publicBaseUrl = publicBaseUrl;
    }

    private WhatsAppConnectionService whatsApp;

    /** Opcional: envia o link pelo WhatsApp conectado da clínica (Evolution). */
    @Autowired(required = false)
    public void setWhatsAppConnectionService(WhatsAppConnectionService whatsApp) {
        this.whatsApp = whatsApp;
    }

    // ------------------------------------------------------------------
    // Rotas autenticadas (ficha do paciente)
    // ------------------------------------------------------------------

    @Transactional
    public CreatedFormRequest create(UUID companyId, UUID contactId, CreateFormRequest request, UUID actorId) {
        String mode = MODE_RECEPTION.equalsIgnoreCase(request.mode()) ? MODE_RECEPTION : MODE_LINK;
        PatientAnamnesisResponse anamnesis = anamnesisUseCase.start(companyId, contactId,
                new StartPatientAnamnesisRequest(request.modelId()), actorId);
        try {
            TenantContext.setCompanyId(companyId);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = MODE_RECEPTION.equals(mode)
                    ? now.plusHours(RECEPTION_HOURS)
                    : now.plusDays(LINK_DAYS);
            String token = RegistrationLink.generateToken();
            PatientFormRequestJpaEntity saved = repository.save(new PatientFormRequestJpaEntity(companyId,
                    contactId, anamnesis.id(), RegistrationLink.hash(token), mode, expiresAt, actorId, now));
            String url = buildUrl(token);
            boolean sent = MODE_LINK.equals(mode) && Boolean.TRUE.equals(request.sendWhatsApp())
                    && sendByWhatsApp(companyId, contactId, anamnesis.modelName(), url);
            TenantContext.setCompanyId(companyId);
            timeline.record(companyId, contactId, (MODE_RECEPTION.equals(mode)
                    ? "Formulário aberto no modo recepção: "
                    : sent ? "Formulário enviado ao paciente pelo WhatsApp: "
                    : "Formulário gerado para o paciente: ") + anamnesis.modelName(), actorId);
            return new CreatedFormRequest(saved.getId(), anamnesis.id(), url, mode, expiresAt, sent);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public List<FormRequestResponse> list(UUID companyId, UUID contactId) {
        contactUseCase.getById(companyId, contactId); // 404 se não for da empresa
        Map<UUID, String> modelNames = new HashMap<>();
        anamnesisUseCase.list(companyId, contactId)
                .forEach(a -> modelNames.put(a.id(), a.modelName()));
        try {
            TenantContext.setCompanyId(companyId);
            LocalDateTime now = LocalDateTime.now();
            return repository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId).stream()
                    .map(r -> new FormRequestResponse(r.getId(), r.getAnamnesisId(),
                            modelNames.get(r.getAnamnesisId()), r.getMode(),
                            r.isPending() && r.isExpired(now) ? "EXPIRED" : r.getStatus(),
                            r.getCreatedAt(), r.getExpiresAt(), r.getAnsweredAt(),
                            r.getSignatureStorageId() == null ? null
                                    : "/api/v1/companies/" + companyId + "/storage/"
                                            + r.getSignatureStorageId() + "?inline=true"))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    // ------------------------------------------------------------------
    // Rotas públicas (paciente, sem login)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PublicFormResponse preview(String token) {
        String hash = RegistrationLink.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            PatientFormRequestJpaEntity request = findUsable(hash);
            PatientAnamnesisResponse anamnesis = anamnesisUseCase.get(request.getCompanyId(),
                    request.getContactId(), request.getAnamnesisId());
            String clinicName = companyRepository.findById(request.getCompanyId())
                    .map(Company::getTradingName)
                    .orElseThrow(RegistrationLinkNotFoundException::new);
            String firstName = contactUseCase.getById(request.getCompanyId(), request.getContactId()).firstName();
            return new PublicFormResponse(clinicName, firstName, anamnesis.modelName(),
                    patientView(anamnesis.snapshot()), request.getMode());
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    @Transactional
    public void submit(String token, SubmitPublicForm form) {
        if (form.website() != null && !form.website().isBlank()) {
            throw new ContactValidationException("Requisição inválida.");
        }
        byte[] signature = decodeSignature(form.signature());
        String hash = RegistrationLink.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            PatientFormRequestJpaEntity request = findUsable(hash);
            UUID companyId = request.getCompanyId();
            UUID contactId = request.getContactId();
            PatientAnamnesisResponse anamnesis = anamnesisUseCase.get(companyId, contactId,
                    request.getAnamnesisId());
            if (anamnesis.status() != PatientAnamnesisStatus.DRAFT) {
                throw new RegistrationLinkNoLongerValidException("Este formulário já foi respondido.");
            }
            AnamnesisModelResponse visible = patientView(anamnesis.snapshot());
            Map<UUID, PatientAnamnesisAnswer> answers = onlyVisible(visible, form.answers());
            requirePatientAnswers(visible, answers);
            boolean professionalPart = hasProfessionalQuestions(anamnesis.snapshot());
            anamnesisUseCase.save(companyId, contactId, anamnesis.id(),
                    new SavePatientAnamnesisRequest(answers, !professionalPart), null, true);

            String fileName = "assinatura-" + anamnesis.modelName().replaceAll("[^\\p{L}\\p{N}]+", "-")
                    .toLowerCase() + "-" + LocalDateTime.now().toLocalDate() + ".png";
            StorageResponse stored = storageUseCase.uploadForContact(companyId, contactId, fileName,
                    "image/png", signature, request.getCreatedBy());

            TenantContext.setCompanyId(companyId);
            request.markAnswered(LocalDateTime.now(), stored.id());
            repository.save(request);
            timeline.record(companyId, contactId, "Formulário respondido e assinado pelo paciente: "
                    + anamnesis.modelName() + (professionalPart ? " (aguarda o profissional concluir)" : ""),
                    null);
            notifySender(request, anamnesis.modelName());
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    // ------------------------------------------------------------------

    /** Falha no WhatsApp não impede o link: a tela oferece copiar/enviar manualmente. */
    private boolean sendByWhatsApp(UUID companyId, UUID contactId, String formName, String url) {
        if (whatsApp == null) {
            return false;
        }
        var contact = contactUseCase.getById(companyId, contactId);
        String phone = contact.mobile() != null ? contact.mobile() : contact.phone();
        String clinic = companyRepository.findById(companyId).map(Company::getTradingName).orElse("a clínica");
        try {
            whatsApp.sendText(companyId, phone, "Olá, " + contact.firstName() + "! " + clinic
                    + " pediu que você responda o formulário \"" + formName + "\" antes do atendimento: "
                    + url + " (o link vale por " + LINK_DAYS + " dias).");
            return true;
        } catch (RuntimeException ex) {
            log.warn("Formulário {} não enviado pelo WhatsApp: {}", formName, ex.getMessage());
            return false;
        }
    }

    private PatientFormRequestJpaEntity findUsable(String hash) {
        PatientFormRequestJpaEntity request = repository.findByTokenHash(hash)
                .orElseThrow(RegistrationLinkNotFoundException::new);
        if (!request.isPending()) {
            throw new RegistrationLinkNoLongerValidException("Este formulário já foi respondido.");
        }
        if (request.isExpired(LocalDateTime.now())) {
            throw new RegistrationLinkNoLongerValidException("Este link de formulário expirou.");
        }
        return request;
    }

    private void notifySender(PatientFormRequestJpaEntity request, String formName) {
        if (request.getCreatedBy() == null) {
            return;
        }
        try {
            String patient = contactUseCase.getById(request.getCompanyId(), request.getContactId()).firstName();
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("kind", "PATIENT_FORM");
            metadata.put("contactId", request.getContactId().toString());
            metadata.put("anamnesisId", request.getAnamnesisId().toString());
            notifications.create(request.getCompanyId(), new CreateNotificationRequest(request.getCreatedBy(),
                    NotificationType.SYSTEM, "Formulário respondido pelo paciente",
                    patient + " respondeu: " + formName, objectMapper.writeValueAsString(metadata)),
                    request.getCreatedBy());
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Não foi possível notificar a resposta do formulário {}", request.getId(), ex);
        }
    }

    /** Só as seções do paciente (sem as "do profissional"). */
    static AnamnesisModelResponse patientView(AnamnesisModelResponse model) {
        List<AnamnesisSectionResponse> sections = model.sections() == null ? List.of()
                : model.sections().stream().filter(s -> !s.professional()).toList();
        return new AnamnesisModelResponse(model.id(), model.companyId(), model.name(), model.description(),
                model.active(), model.isDefault(), model.version(), sections, model.createdAt(),
                model.updatedAt());
    }

    private static boolean hasProfessionalQuestions(AnamnesisModelResponse model) {
        return model.sections() != null && model.sections().stream()
                .anyMatch(s -> s.professional() && s.questions() != null && !s.questions().isEmpty());
    }

    private static Map<UUID, PatientAnamnesisAnswer> onlyVisible(AnamnesisModelResponse visible,
                                                                  Map<UUID, PatientAnamnesisAnswer> answers) {
        Map<UUID, PatientAnamnesisAnswer> result = new HashMap<>();
        for (AnamnesisSectionResponse section : visible.sections()) {
            for (AnamnesisQuestionResponse question : section.questions() == null
                    ? List.<AnamnesisQuestionResponse>of() : section.questions()) {
                PatientAnamnesisAnswer answer = answers.get(question.id());
                if (answer != null) {
                    result.put(question.id(), answer);
                }
            }
        }
        return result;
    }

    private static void requirePatientAnswers(AnamnesisModelResponse visible,
                                              Map<UUID, PatientAnamnesisAnswer> answers) {
        List<String> missing = visible.sections().stream()
                .flatMap(s -> s.questions() == null ? java.util.stream.Stream.empty() : s.questions().stream())
                .filter(AnamnesisQuestionResponse::required)
                .filter(q -> isBlank(answers.get(q.id())))
                .map(AnamnesisQuestionResponse::text)
                .toList();
        if (!missing.isEmpty()) {
            throw new ContactValidationException("Responda as perguntas obrigatórias: "
                    + String.join("; ", missing.stream().limit(3).toList())
                    + (missing.size() > 3 ? " e mais " + (missing.size() - 3) : "") + ".");
        }
    }

    private static boolean isBlank(PatientAnamnesisAnswer answer) {
        if (answer == null || answer.value() == null) {
            return true;
        }
        Object value = answer.value();
        return (value instanceof String s && s.isBlank())
                || (value instanceof java.util.Collection<?> c && c.isEmpty());
    }

    static byte[] decodeSignature(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith(PNG_PREFIX)) {
            throw new ContactValidationException("Assine no quadro antes de enviar.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(PNG_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new ContactValidationException("Assinatura inválida.");
        }
        // Assinatura em branco gera um PNG minúsculo; PNG real começa com 0x89 'P' 'N' 'G'.
        if (bytes.length < 200 || bytes.length > MAX_SIGNATURE_BYTES
                || bytes[0] != (byte) 0x89 || bytes[1] != 'P' || bytes[2] != 'N' || bytes[3] != 'G') {
            throw new ContactValidationException("Assinatura inválida.");
        }
        return bytes;
    }

    private String buildUrl(String token) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        return base + "/f/" + token;
    }
}
