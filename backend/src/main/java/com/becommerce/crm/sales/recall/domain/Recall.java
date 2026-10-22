package com.becommerce.crm.sales.recall.domain;

import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Recall {

    private static final int PHONE_MAX_LENGTH = 20;
    private static final int TAG_MAX_LENGTH = 120;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private String phone;
    private UUID hostId;
    private String tag;
    private UUID tagId;
    private LocalDate dueDate;
    private RecallStatus status;
    private UUID appointmentId;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private Recall(UUID id, UUID companyId, UUID contactId, String phone, UUID hostId, String tag,
                   LocalDate dueDate, RecallStatus status, UUID appointmentId, UUID createdBy,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.phone = phone;
        this.hostId = hostId;
        this.tag = tag;
        this.dueDate = dueDate;
        this.status = status;
        this.appointmentId = appointmentId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Recall create(UUID companyId, UUID contactId, String phone, UUID hostId, String tag,
                                LocalDate dueDate, UUID createdBy) {
        if (companyId == null) {
            throw new RecallValidationException("Empresa é obrigatória.");
        }
        if (contactId == null) {
            throw new RecallValidationException("Paciente é obrigatório.");
        }
        if (hostId == null) {
            throw new RecallValidationException("Dentista é obrigatório.");
        }
        validatePhone(phone);
        validateTag(tag);
        if (dueDate == null) {
            throw new RecallValidationException("Data prevista do retorno é obrigatória.");
        }
        Instant now = Instant.now();
        return new Recall(UUID.randomUUID(), companyId, contactId, phone.trim(), hostId, normalizeTag(tag),
                dueDate, RecallStatus.PENDING, null, createdBy, now, now);
    }

    public static Recall reconstitute(UUID id, UUID companyId, UUID contactId, String phone, UUID hostId,
                                      String tag, LocalDate dueDate, RecallStatus status, UUID appointmentId,
                                      UUID createdBy, Instant createdAt, Instant updatedAt) {
        return new Recall(id, companyId, contactId, phone, hostId, tag, dueDate, status, appointmentId,
                createdBy, createdAt, updatedAt);
    }

    public static LocalDate dueDateFrom(LocalDate today, int amount, RecallDueUnit unit) {
        if (today == null) {
            throw new RecallValidationException("Data base é obrigatória.");
        }
        if (amount < 1) {
            throw new RecallValidationException("Prazo deve ser maior que zero.");
        }
        if (unit == null) {
            throw new RecallValidationException("Unidade do prazo é obrigatória.");
        }
        return switch (unit) {
            case DAYS -> today.plusDays(amount);
            case WEEKS -> today.plusWeeks(amount);
            case MONTHS -> today.plusMonths(amount);
            case YEARS -> today.plusYears(amount);
        };
    }

    /**
     * Vincula o marcador cadastrado (tabela tags, V101). O texto {@code tag} acompanha o nome
     * para manter compatíveis os retornos antigos, que só tinham texto livre.
     */
    public void linkTag(UUID tagId, String tagName) {
        if (status == RecallStatus.DISMISSED) {
            throw new RecallValidationException("Retorno descartado não pode ser alterado.");
        }
        this.tagId = tagId;
        this.tag = tagId == null ? null : normalizeTag(tagName);
        touch();
    }

    /** Usado só ao reconstituir do banco. */
    public void restoreTagId(UUID tagId) {
        this.tagId = tagId;
    }

    public UUID getTagId() {
        return tagId;
    }

    public void update(String phone, UUID hostId, String tag, LocalDate dueDate) {
        if (status == RecallStatus.DISMISSED) {
            throw new RecallValidationException("Retorno descartado não pode ser alterado.");
        }
        if (phone != null) {
            validatePhone(phone);
            this.phone = phone.trim();
        }
        if (hostId != null) {
            this.hostId = hostId;
        }
        if (tag != null) {
            validateTag(tag);
            this.tag = normalizeTag(tag);
        }
        if (dueDate != null) {
            this.dueDate = dueDate;
        }
        touch();
    }

    public void dismiss() {
        if (status == RecallStatus.DISMISSED) {
            return;
        }
        this.status = RecallStatus.DISMISSED;
        this.appointmentId = null;
        touch();
    }

    public void schedule(UUID appointmentId) {
        if (status == RecallStatus.DISMISSED) {
            throw new RecallValidationException("Retorno descartado não pode ser agendado.");
        }
        if (status == RecallStatus.SCHEDULED) {
            throw new RecallValidationException("Retorno já está agendado.");
        }
        if (appointmentId == null) {
            throw new RecallValidationException("Agendamento é obrigatório.");
        }
        this.status = RecallStatus.SCHEDULED;
        this.appointmentId = appointmentId;
        touch();
    }

    private static void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new RecallValidationException("Celular é obrigatório.");
        }
        if (phone.trim().length() > PHONE_MAX_LENGTH) {
            throw new RecallValidationException("Celular deve ter no máximo " + PHONE_MAX_LENGTH + " caracteres.");
        }
    }

    private static void validateTag(String tag) {
        if (tag != null && tag.trim().length() > TAG_MAX_LENGTH) {
            throw new RecallValidationException("Marcador deve ter no máximo " + TAG_MAX_LENGTH + " caracteres.");
        }
    }

    private static String normalizeTag(String tag) {
        return tag == null || tag.isBlank() ? null : tag.trim();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public String getPhone() {
        return phone;
    }

    public UUID getHostId() {
        return hostId;
    }

    public String getTag() {
        return tag;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public RecallStatus getStatus() {
        return status;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
