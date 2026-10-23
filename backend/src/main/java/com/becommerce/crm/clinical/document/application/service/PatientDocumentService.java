package com.becommerce.crm.clinical.document.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.document.application.dto.IssuePatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentPdf;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.port.in.PatientDocumentUseCase;
import com.becommerce.crm.clinical.document.application.port.out.PatientDocumentRepository;
import com.becommerce.crm.clinical.document.domain.DocumentPlaceholderRenderer;
import com.becommerce.crm.clinical.document.domain.PatientDocument;
import com.becommerce.crm.clinical.document.domain.exception.PatientDocumentNotFoundException;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateResponse;
import com.becommerce.crm.clinical.documenttemplate.application.port.in.DocumentTemplateUseCase;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Documentos emitidos para o paciente (ficha do paciente, Fase 2). O preview
 * renderiza o modelo sem gravar; a emissão guarda o texto final exatamente como
 * o profissional confirmou. Exclusão é restrita a administradores.
 */
@Service
public class PatientDocumentService implements PatientDocumentUseCase {

    private static final String RESOURCE = "PatientDocument";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final String[] MONTHS = {
            "janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho",
            "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private final PatientDocumentRepository documentRepository;
    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final DocumentTemplateUseCase templateUseCase;
    private final UserRepository userRepository;
    private final PatientDocumentPdfRenderer pdfRenderer;
    private final TenantAuditRecorder auditor;

    public PatientDocumentService(PatientDocumentRepository documentRepository,
                                  ContactRepository contactRepository,
                                  CompanyRepository companyRepository,
                                  DocumentTemplateUseCase templateUseCase,
                                  UserRepository userRepository,
                                  PatientDocumentPdfRenderer pdfRenderer,
                                  TenantAuditRecorder auditor) {
        this.documentRepository = documentRepository;
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
        this.templateUseCase = templateUseCase;
        this.userRepository = userRepository;
        this.pdfRenderer = pdfRenderer;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDocumentResponse> list(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Map<UUID, String> names = new HashMap<>();
            return documentRepository.findByCompanyIdAndContactIdOrderByIssuedAtDesc(companyId, contactId)
                    .stream()
                    .map(document -> toResponse(document, names))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewPatientDocumentResponse preview(UUID companyId, UUID contactId,
                                                  PreviewPatientDocumentRequest request, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            Contact contact = requireOwnedContact(companyId, contactId);
            DocumentTemplateResponse template = templateUseCase.get(companyId, request.templateId());
            if (!template.active()) {
                throw new ContactValidationException("Este modelo de documento está inativo.");
            }
            String body = DocumentPlaceholderRenderer.render(template.body(),
                    placeholders(contact, companyId, actorId));
            return new PreviewPatientDocumentResponse(template.type(), template.title(), body);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientDocumentResponse issue(UUID companyId, UUID contactId,
                                         IssuePatientDocumentRequest request, UUID issuedBy) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            UUID templateId = request.templateId();
            if (templateId != null) {
                templateUseCase.get(companyId, templateId);
            }
            PatientDocument document = PatientDocument.issue(companyId, contactId, templateId,
                    request.type(), request.title(), request.body(),
                    request.professionalName(), request.professionalRegistry(), issuedBy);
            documentRepository.save(document);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, RESOURCE,
                    document.getId().toString(), "Documento emitido: " + document.getTitle() + ".",
                    issuedBy, null);
            return toResponse(document, new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDocumentPdf pdf(UUID companyId, UUID contactId, UUID documentId) {
        try {
            TenantContext.setCompanyId(companyId);
            Contact contact = requireOwnedContact(companyId, contactId);
            PatientDocument document = requireOwned(companyId, contactId, documentId);
            String companyName = companyRepository.findById(companyId)
                    .map(Company::getTradingName).orElse("");
            byte[] content = pdfRenderer.render(document, companyName, fullName(contact));
            return new PatientDocumentPdf(content, fileName(document, contact));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID documentId, boolean admin, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientDocument document = requireOwned(companyId, contactId, documentId);
            if (!admin) {
                throw new CrmAccessDeniedException("Só administradores podem excluir documentos emitidos.");
            }
            documentRepository.delete(document);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, RESOURCE,
                    documentId.toString(), "Documento excluído.", actorId, null);
        } finally {
            TenantContext.clear();
        }
    }

    private Contact requireOwnedContact(UUID companyId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
        return contact;
    }

    private PatientDocument requireOwned(UUID companyId, UUID contactId, UUID documentId) {
        PatientDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new PatientDocumentNotFoundException(documentId));
        if (!document.getCompanyId().equals(companyId)
                || !document.getContactId().equals(contactId)) {
            throw new PatientDocumentNotFoundException(documentId);
        }
        return document;
    }

    private Map<String, String> placeholders(Contact contact, UUID companyId, UUID actorId) {
        LocalDate today = LocalDate.now(SAO_PAULO);
        Map<String, String> values = new HashMap<>();
        values.put("paciente.nome", fullName(contact));
        values.put("paciente.cpf", formatCpf(contact.getCpf()));
        values.put("paciente.nascimento",
                contact.getBirthDate() == null ? "" : DATE.format(contact.getBirthDate()));
        values.put("paciente.prontuario", contact.getRecordNumber() == null ? "" : contact.getRecordNumber());
        values.put("data", DATE.format(today));
        values.put("data.extenso", today.getDayOfMonth() + " de " + MONTHS[today.getMonthValue() - 1]
                + " de " + today.getYear());
        values.put("cidade", contact.getCity() == null ? "" : contact.getCity());
        values.put("empresa.nome", companyRepository.findById(companyId)
                .map(Company::getTradingName).orElse(""));
        values.put("profissional.nome", actorName(actorId));
        return values;
    }

    private String actorName(UUID actorId) {
        if (actorId == null) {
            return "";
        }
        return userRepository.findById(actorId).map(User::getName).orElse("");
    }

    private static String fullName(Contact contact) {
        String firstName = contact.getFirstName() == null ? "" : contact.getFirstName().trim();
        String lastName = contact.getLastName() == null ? "" : contact.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return "";
        }
        String digits = NON_DIGITS.matcher(cpf).replaceAll("");
        if (digits.length() != 11) {
            return cpf.trim();
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9)
                + "-" + digits.substring(9);
    }

    private static String fileName(PatientDocument document, Contact contact) {
        String surname = contact.getLastName() == null || contact.getLastName().isBlank()
                ? contact.getFirstName() : contact.getLastName();
        String slug = slug(surname);
        if (slug.isEmpty()) {
            slug = "paciente";
        }
        return document.getType().name().toLowerCase(Locale.ROOT) + "-" + slug + "-"
                + DATE.format(document.getIssuedAt().toLocalDate()).replace('/', '-') + ".pdf";
    }

    private static String slug(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return NON_ALNUM.matcher(normalized).replaceAll("-").replaceAll("^-+|-+$", "");
    }

    private PatientDocumentResponse toResponse(PatientDocument document, Map<UUID, String> names) {
        return new PatientDocumentResponse(
                document.getId(),
                document.getContactId(),
                document.getTemplateId(),
                document.getType(),
                document.getTitle(),
                document.getBody(),
                document.getProfessionalName(),
                document.getProfessionalRegistry(),
                document.getIssuedBy(),
                userName(document.getIssuedBy(), names),
                document.getIssuedAt());
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
