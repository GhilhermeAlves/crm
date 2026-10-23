package com.becommerce.crm.masterdata.contact.registration.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.masterdata.contact.registration.application.dto.PatientRegistrationRequest;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationLinkResponse;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationPreviewResponse;
import com.becommerce.crm.masterdata.contact.registration.application.port.RegistrationLinkRepository;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNoLongerValidException;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNotFoundException;
import com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence.RegistrationTokenContextHolder;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Link público de cadastro do paciente (V108).
 *
 * <p>Rotas públicas (sem sessão) seguem o padrão do convite: o hash do token vai
 * para o GUC transacional via {@link RegistrationTokenContextHolder}; as policies
 * RLS de token liberam só aquela linha de {@code patient_registration_links}; a
 * empresa/contato vêm da própria linha e então o {@link TenantContext} é fixado
 * para ler/atualizar o contato. O RLS não valida validade: expiração e uso são
 * checados aqui.
 */
@Service
public class RegistrationLinkService {

    static final String AUDIT_DESCRIPTION = "Cadastro preenchido pelo paciente pelo link.";

    private final RegistrationLinkRepository repository;
    private final RegistrationTokenContextHolder tokenContext;
    private final ContactUseCase contactUseCase;
    private final CompanyRepository companyRepository;
    private final TenantAuditRecorder auditor;
    private final String publicBaseUrl;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RegistrationLinkService(RegistrationLinkRepository repository,
                                   RegistrationTokenContextHolder tokenContext,
                                   ContactUseCase contactUseCase,
                                   CompanyRepository companyRepository,
                                   TenantAuditRecorder auditor,
                                   @Value("${app.mail.frontend-base-url:}") String publicBaseUrl) {
        this(repository, tokenContext, contactUseCase, companyRepository, auditor, publicBaseUrl,
                Clock.systemDefaultZone());
    }

    RegistrationLinkService(RegistrationLinkRepository repository,
                            RegistrationTokenContextHolder tokenContext,
                            ContactUseCase contactUseCase,
                            CompanyRepository companyRepository,
                            TenantAuditRecorder auditor,
                            String publicBaseUrl,
                            Clock clock) {
        this.repository = repository;
        this.tokenContext = tokenContext;
        this.contactUseCase = contactUseCase;
        this.companyRepository = companyRepository;
        this.auditor = auditor;
        this.publicBaseUrl = publicBaseUrl;
        this.clock = clock;
    }

    /** Gera um novo link para o contato; os links ainda não usados deixam de valer. */
    @Transactional
    public RegistrationLinkResponse create(UUID companyId, UUID contactId, UUID actorId) {
        try {
            contactUseCase.getById(companyId, contactId); // 404 se não existir na empresa
            TenantContext.setCompanyId(companyId);
            LocalDateTime now = LocalDateTime.now(clock);
            repository.invalidateUnused(companyId, contactId, now);
            String token = RegistrationLink.generateToken();
            RegistrationLink link = repository.save(
                    RegistrationLink.create(companyId, contactId, RegistrationLink.hash(token), actorId, now));
            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, "Contact",
                    contactId.toString(), "Link de cadastro gerado para o paciente.",
                    actorId, Map.of("registrationLinkId", link.getId().toString()));
            return new RegistrationLinkResponse(buildUrl(token), link.getExpiresAt());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public RegistrationPreviewResponse preview(String token) {
        String hash = RegistrationLink.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            RegistrationLink link = repository.findByTokenHash(hash)
                    .orElseThrow(RegistrationLinkNotFoundException::new);
            assertUsable(link);
            String companyName = companyRepository.findById(link.getCompanyId())
                    .map(Company::getTradingName)
                    .orElseThrow(RegistrationLinkNotFoundException::new);
            ContactResponse contact = loadContact(link);
            String phone = contact.mobile() != null ? contact.mobile() : contact.phone();
            return new RegistrationPreviewResponse(companyName, contact.firstName(), contact.lastName(), phone);
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    @Transactional
    public void submit(String token, PatientRegistrationRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            throw new ContactValidationException("Requisição inválida.");
        }
        validate(request);
        String hash = RegistrationLink.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            RegistrationLink link = repository.findByTokenHashForUpdate(hash)
                    .orElseThrow(RegistrationLinkNotFoundException::new);
            assertUsable(link);
            try {
                contactUseCase.updateFromRegistrationLink(link.getCompanyId(), link.getContactId(),
                        toUpdateRequest(request));
            } catch (ContactNotFoundException e) {
                throw new RegistrationLinkNotFoundException();
            }
            repository.markUsed(link.getId(), LocalDateTime.now(clock));
            TenantContext.setCompanyId(link.getCompanyId());
            auditor.record(link.getCompanyId(), AuditAction.UPDATE, AuditModule.CONTACTS, "Contact",
                    link.getContactId().toString(), AUDIT_DESCRIPTION, null,
                    Map.of("registrationLinkId", link.getId().toString()));
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    private ContactResponse loadContact(RegistrationLink link) {
        try {
            return contactUseCase.getById(link.getCompanyId(), link.getContactId());
        } catch (ContactNotFoundException e) {
            throw new RegistrationLinkNotFoundException();
        }
    }

    private void assertUsable(RegistrationLink link) {
        if (link.isUsed()) {
            throw new RegistrationLinkNoLongerValidException("Este link de cadastro já foi utilizado.");
        }
        if (link.isExpired(LocalDateTime.now(clock))) {
            throw new RegistrationLinkNoLongerValidException("Este link de cadastro expirou.");
        }
    }

    private String buildUrl(String token) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        return base + "/cadastro/" + token;
    }

    private static void validate(PatientRegistrationRequest request) {
        String cpf = digits(request.cpf());
        if (cpf != null && !isValidCpf(cpf)) {
            throw new ContactValidationException("CPF inválido.");
        }
        String phone = digits(request.phone());
        if (phone != null && (phone.length() < 10 || phone.length() > 13)) {
            throw new ContactValidationException("Telefone inválido.");
        }
        if (request.birthDate() != null && request.birthDate().isAfter(java.time.LocalDate.now())) {
            throw new ContactValidationException("A data de nascimento não pode ser futura.");
        }
    }

    private static UpdateContactRequest toUpdateRequest(PatientRegistrationRequest r) {
        PatientRegistrationRequest.Address a = r.address();
        return new UpdateContactRequest(
                r.firstName().trim(), trim(r.lastName()), trim(r.email()), null, digits(r.phone()), null,
                r.birthDate(), digits(r.cpf()), null, null, null, null, null,
                a == null ? null : digits(a.zipCode()),
                a == null ? null : trim(a.street()),
                a == null ? null : trim(a.number()),
                a == null ? null : trim(a.complement()),
                a == null ? null : trim(a.neighborhood()),
                a == null ? null : trim(a.city()),
                a == null ? null : trim(a.state()),
                null, null, null, null, null, null, null, null);
    }

    private static String trim(String value) {
        // Campo vazio do paciente = "não alterar" (no update interno "" apagaria o dado).
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String digits(String value) {
        if (value == null) {
            return null;
        }
        String d = value.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    /** Dígitos verificadores do CPF (11 dígitos, não todos iguais). */
    static boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        for (int len = 9; len <= 10; len++) {
            int sum = 0;
            for (int i = 0; i < len; i++) {
                sum += (cpf.charAt(i) - '0') * (len + 1 - i);
            }
            int check = (sum * 10) % 11 % 10;
            if (check != cpf.charAt(len) - '0') {
                return false;
            }
        }
        return true;
    }
}
