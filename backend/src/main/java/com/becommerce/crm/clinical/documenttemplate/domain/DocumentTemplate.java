package com.becommerce.crm.clinical.documenttemplate.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de documento da empresa (Configurações -> Documentos). O corpo pode
 * conter variáveis {@code {{...}}} resolvidas na emissão. Corresponde à tabela
 * {@code document_templates} (V105) e pertence a uma única empresa (RLS FORCE).
 */
public class DocumentTemplate {

    public static final int MAX_TITLE_LENGTH = 160;
    public static final int MAX_BODY_LENGTH = 20000;

    private final UUID id;
    private final UUID companyId;
    private DocumentType type;
    private String title;
    private String body;
    private boolean active;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DocumentTemplate(UUID id, UUID companyId, DocumentType type, String title, String body,
                             boolean active, UUID createdBy, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.active = active;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentTemplate create(UUID companyId, DocumentType type, String title, String body,
                                          UUID createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return new DocumentTemplate(UUID.randomUUID(), companyId, requireType(type),
                normalizeTitle(title), normalizeBody(body), true, createdBy, now, now);
    }

    public static DocumentTemplate reconstitute(UUID id, UUID companyId, DocumentType type, String title,
                                                String body, boolean active, UUID createdBy,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DocumentTemplate(id, companyId, type, title, body, active, createdBy, createdAt, updatedAt);
    }

    public void edit(DocumentType type, String title, String body, Boolean active) {
        if (type != null) {
            this.type = requireType(type);
        }
        this.title = normalizeTitle(title);
        this.body = normalizeBody(body);
        if (active != null) {
            this.active = active;
        }
        this.updatedAt = LocalDateTime.now();
    }

    private static DocumentType requireType(DocumentType type) {
        if (type == null) {
            throw new ContactValidationException("O tipo do documento é obrigatório.");
        }
        return type;
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ContactValidationException("O título do modelo não pode ser vazio.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new ContactValidationException(
                    "O título do modelo deve ter no máximo " + MAX_TITLE_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    private static String normalizeBody(String body) {
        if (body == null || body.isBlank()) {
            throw new ContactValidationException("O corpo do modelo não pode ser vazio.");
        }
        String trimmed = body.trim();
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new ContactValidationException(
                    "O corpo do modelo deve ter no máximo " + MAX_BODY_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public DocumentType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isActive() { return active; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
