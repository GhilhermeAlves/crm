package com.becommerce.crm.masterdata.contact.note.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Anotação do paciente (ficha do paciente, Fase 1). Pertence a uma empresa
 * (company_id, RLS FORCE) e a um contato da MESMA empresa (validado no serviço).
 * Corresponde à tabela {@code contact_notes} (V097).
 *
 * <p>Distinta do campo legado {@code contacts.notes}: aqui há uma lista com autor
 * e data, editável apenas pelo autor ou por administradores (regra no serviço).
 */
public class ContactNote {

    public static final int MAX_BODY_LENGTH = 5000;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private String body;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ContactNote(UUID id, UUID companyId, UUID contactId, String body, UUID createdBy,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.body = body;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContactNote create(UUID companyId, UUID contactId, String body, UUID createdBy) {
        String normalized = normalize(body);
        LocalDateTime now = LocalDateTime.now();
        return new ContactNote(UUID.randomUUID(), companyId, contactId, normalized, createdBy, now, now);
    }

    public static ContactNote reconstitute(UUID id, UUID companyId, UUID contactId, String body,
                                           UUID createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ContactNote(id, companyId, contactId, body, createdBy, createdAt, updatedAt);
    }

    public void edit(String newBody) {
        this.body = normalize(newBody);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAuthoredBy(UUID userId) {
        return createdBy != null && createdBy.equals(userId);
    }

    private static String normalize(String body) {
        if (body == null || body.isBlank()) {
            throw new ContactValidationException("A anotação não pode ser vazia.");
        }
        String trimmed = body.trim();
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new ContactValidationException(
                    "A anotação deve ter no máximo " + MAX_BODY_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public String getBody() { return body; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
