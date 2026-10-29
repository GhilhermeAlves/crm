package com.becommerce.crm.clinical.anamnesis.formrequest;

import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.CreateFormRequest;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.OpenLinkResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.PublicFormResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.SubmitOpenForm;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.SubmitPublicForm;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.service.AnamnesisService;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNoLongerValidException;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Link aberto de um modelo de anamnese: endereço fixo ({@code /f/a/{token}}) que a
 * clínica divulga por qualquer canal. Quem responde informa nome e telefone; o
 * paciente é achado pelo telefone (ou cadastrado) e a resposta segue o fluxo do
 * formulário por link ({@link PatientFormRequestService}): assinatura, timeline e
 * notificação a quem gerou o link.
 */
@Service
public class OpenFormService {

    static final String MODE_OPEN = "OPEN";

    private final AnamnesisOpenLinkJpaRepository repository;
    private final FormTokenContextHolder tokenContext;
    private final PatientFormRequestService formRequests;
    private final AnamnesisService models;
    private final ContactUseCase contacts;
    private final CompanyRepository companyRepository;
    private final String publicBaseUrl;

    public OpenFormService(AnamnesisOpenLinkJpaRepository repository, FormTokenContextHolder tokenContext,
                           PatientFormRequestService formRequests, AnamnesisService models,
                           ContactUseCase contacts, CompanyRepository companyRepository,
                           @Value("${app.mail.frontend-base-url:}") String publicBaseUrl) {
        this.repository = repository;
        this.tokenContext = tokenContext;
        this.formRequests = formRequests;
        this.models = models;
        this.contacts = contacts;
        this.companyRepository = companyRepository;
        this.publicBaseUrl = publicBaseUrl;
    }

    /** Devolve o link do modelo, criando-o na primeira vez. */
    @Transactional
    public OpenLinkResponse getOrCreate(UUID companyId, UUID modelId, UUID actorId) {
        models.getById(companyId, modelId); // 404 se não for da empresa
        try {
            TenantContext.setCompanyId(companyId);
            AnamnesisOpenLinkJpaEntity link = repository.findByCompanyIdAndModelId(companyId, modelId)
                    .orElseGet(() -> repository.save(new AnamnesisOpenLinkJpaEntity(companyId, modelId,
                            RegistrationLink.generateToken(), actorId)));
            return new OpenLinkResponse(modelId, buildUrl(link.getToken()));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public PublicFormResponse preview(String token) {
        try {
            AnamnesisOpenLinkJpaEntity link = findLink(token);
            AnamnesisModelResponse model = activeModel(link);
            Company company = companyRepository.findById(link.getCompanyId())
                    .orElseThrow(RegistrationLinkNotFoundException::new);
            return new PublicFormResponse(company.getTradingName(), company.getLogoUrl(), null, model.name(),
                    PatientFormRequestService.patientView(model), MODE_OPEN);
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    @Transactional
    public void submit(String token, SubmitOpenForm form) {
        if (form.website() != null && !form.website().isBlank()) {
            throw new ContactValidationException("Requisição inválida.");
        }
        PatientFormRequestService.decodeSignature(form.signature()); // falha antes de cadastrar
        String phone = form.phone().replaceAll("\\D", "");
        if (phone.length() < 10 || phone.length() > 13) {
            throw new ContactValidationException("Informe um telefone válido com DDD.");
        }
        AnamnesisOpenLinkJpaEntity link;
        try {
            link = findLink(token);
        } finally {
            tokenContext.clear();
        }
        activeModel(link);
        UUID companyId = link.getCompanyId();
        UUID contactId = findOrCreatePatient(companyId, form, phone, link.getCreatedBy());
        String url = formRequests.create(companyId, contactId,
                new CreateFormRequest(link.getModelId(), PatientFormRequestService.MODE_LINK, false, false),
                link.getCreatedBy()).url();
        String formToken = url.substring(url.lastIndexOf('/') + 1);
        formRequests.submit(formToken, new SubmitPublicForm(form.answers(), form.signature(), null));
    }

    /** Mesmo telefone = mesma pessoa; senão cadastra um paciente novo. */
    private UUID findOrCreatePatient(UUID companyId, SubmitOpenForm form, String phone, UUID actorId) {
        String key = phoneKey(phone);
        ContactResponse existing = contacts.search(companyId, phone.substring(phone.length() - 8), 50).stream()
                .filter(c -> key.equals(phoneKey(c.mobile())) || key.equals(phoneKey(c.phone())))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            contacts.makePatient(companyId, existing.id());
            return existing.id();
        }
        CreateContactRequest request = new CreateContactRequest(form.firstName().trim(),
                blankToNull(form.lastName()), null, null, phone, "Cadastrado pelo link aberto do formulário.",
                form.birthDate(), blankToNull(form.cpf()), null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
        return contacts.createPatient(companyId, request, actorId).id();
    }

    /**
     * DDD + 8 últimos dígitos: ignora o 55 e o 9 extra do celular, que às vezes
     * estão gravados e às vezes não.
     */
    static String phoneKey(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() >= 12 && digits.startsWith("55")) {
            digits = digits.substring(2);
        }
        if (digits.length() < 10) {
            return null;
        }
        return digits.substring(0, 2) + digits.substring(digits.length() - 8);
    }

    private AnamnesisOpenLinkJpaEntity findLink(String token) {
        if (token == null || token.isBlank() || token.length() > 64) {
            throw new RegistrationLinkNotFoundException();
        }
        tokenContext.setTokenHash(token);
        return repository.findByToken(token).orElseThrow(RegistrationLinkNotFoundException::new);
    }

    private AnamnesisModelResponse activeModel(AnamnesisOpenLinkJpaEntity link) {
        AnamnesisModelResponse model = models.getById(link.getCompanyId(), link.getModelId());
        if (!model.active()) {
            throw new RegistrationLinkNoLongerValidException("Este formulário não está mais disponível.");
        }
        return model;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildUrl(String token) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        return base + "/f/a/" + token;
    }
}
