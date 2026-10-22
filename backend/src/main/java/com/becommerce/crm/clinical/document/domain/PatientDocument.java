package com.becommerce.crm.clinical.document.domain;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Documento emitido para o paciente (ficha do paciente, Fase 2). Guarda o corpo
 * final exatamente como o profissional confirmou; editar ou excluir o modelo
 * depois não altera o documento (a FK de {@code template_id} é {@code SET NULL}).
 */
public class PatientDocument {

    static final int MAX_TITLE_LENGTH = 160;
    static final int MAX_BODY_LENGTH = 20000;
    static final int MAX_PROFESSIONAL_NAME_LENGTH = 160;
    static final int MAX_PROFESSIONAL_REGISTRY_LENGTH = 40;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private final UUID templateId;
    private final DocumentType type;
    private final String title;
    private final String body;
    private final String professionalName;
    private final String professionalRegistry;
    private final UUID issuedBy;
    private final LocalDateTime issuedAt;

    private PatientDocument(UUID id, UUID companyId, UUID contactId, UUID templateId, DocumentType type,
                            String title, String body, String professionalName, String professionalRegistry,
                            UUID issuedBy, LocalDateTime issuedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.templateId = templateId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.professionalName = professionalName;
        this.professionalRegistry = professionalRegistry;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
    }

    public static PatientDocument issue(UUID companyId, UUID contactId, UUID templateId, DocumentType type,
                                        String title, String body, String professionalName,
                                        String professionalRegistry, UUID issuedBy) {
        if (type == null) {
            throw new ContactValidationException("O tipo do documento é obrigatório.");
        }
        if (professionalRegistry != null
                && professionalRegistry.trim().length() > MAX_PROFESSIONAL_REGISTRY_LENGTH) {
            throw new ContactValidationException(
                    "O registro profissional deve ter no máximo " + MAX_PROFESSIONAL_REGISTRY_LENGTH
                            + " caracteres.");
        }
        return new PatientDocument(UUID.randomUUID(), companyId, contactId, templateId, type,
                required(title, "O título do documento é obrigatório.", MAX_TITLE_LENGTH,
                        "O título do documento deve ter no máximo " + MAX_TITLE_LENGTH + " caracteres."),
                bodyValue(body),
                required(professionalName, "O nome do profissional é obrigatório.",
                        MAX_PROFESSIONAL_NAME_LENGTH,
                        "O nome do profissional deve ter no máximo " + MAX_PROFESSIONAL_NAME_LENGTH
                                + " caracteres."),
                professionalRegistry == null || professionalRegistry.isBlank()
                        ? null : professionalRegistry.trim(),
                issuedBy, LocalDateTime.now());
    }

    public static PatientDocument reconstitute(UUID id, UUID companyId, UUID contactId, UUID templateId,
                                               DocumentType type, String title, String body,
                                               String professionalName, String professionalRegistry,
                                               UUID issuedBy, LocalDateTime issuedAt) {
        return new PatientDocument(id, companyId, contactId, templateId, type, title, body,
                professionalName, professionalRegistry, issuedBy, issuedAt);
    }

    private static String bodyValue(String body) {
        if (body == null || body.isBlank()) {
            throw new ContactValidationException("O texto do documento é obrigatório.");
        }
        if (body.length() > MAX_BODY_LENGTH) {
            throw new ContactValidationException(
                    "O texto do documento deve ter no máximo " + MAX_BODY_LENGTH + " caracteres.");
        }
        return body;
    }

    private static String required(String value, String blankMessage, int maxLength, String longMessage) {
        if (value == null || value.isBlank()) {
            throw new ContactValidationException(blankMessage);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ContactValidationException(longMessage);
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getTemplateId() { return templateId; }
    public DocumentType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getProfessionalName() { return professionalName; }
    public String getProfessionalRegistry() { return professionalRegistry; }
    public UUID getIssuedBy() { return issuedBy; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}
