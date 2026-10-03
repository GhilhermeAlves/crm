package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;

import java.time.Instant;
import java.util.UUID;

public class Appointment {

    private final UUID id;
    private final UUID companyId;
    private UUID appointmentTypeId;
    private UUID hostId;
    private UUID contactId;
    private UUID opportunityId;
    private String title;
    private Instant startAt;
    private Instant endAt;
    private AppointmentStatus status;
    private AppointmentSource source;
    private LocationKind locationKind;
    private String locationDetail;
    private String meetingUrl;
    private String notes;
    private String cancelReason;
    private final String publicToken;
    private String googleEventId;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private Appointment(UUID id, UUID companyId, UUID appointmentTypeId, UUID hostId,
                        UUID contactId, UUID opportunityId, String title,
                        Instant startAt, Instant endAt, AppointmentStatus status,
                        AppointmentSource source, LocationKind locationKind, String locationDetail,
                        String meetingUrl, String notes, String cancelReason,
                        String publicToken, String googleEventId, UUID createdBy,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.appointmentTypeId = appointmentTypeId;
        this.hostId = hostId;
        this.contactId = contactId;
        this.opportunityId = opportunityId;
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.source = source;
        this.locationKind = locationKind;
        this.locationDetail = locationDetail;
        this.meetingUrl = meetingUrl;
        this.notes = notes;
        this.cancelReason = cancelReason;
        this.publicToken = publicToken;
        this.googleEventId = googleEventId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Appointment create(UUID companyId, UUID appointmentTypeId, UUID hostId,
                                     UUID contactId, UUID opportunityId, String title,
                                     Instant startAt, Instant endAt, AppointmentSource source,
                                     LocationKind locationKind, String locationDetail,
                                     String notes, UUID createdBy) {
        validateTitle(title);
        validateRange(startAt, endAt);
        Instant now = Instant.now();
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        return new Appointment(UUID.randomUUID(), companyId, appointmentTypeId, hostId,
                contactId, opportunityId, title.trim(), startAt, endAt,
                AppointmentStatus.SCHEDULED, source != null ? source : AppointmentSource.INTERNAL,
                locationKind, locationDetail, null, notes, null,
                token, null, createdBy, now, now);
    }

    public static Appointment reconstitute(UUID id, UUID companyId, UUID appointmentTypeId, UUID hostId,
                                           UUID contactId, UUID opportunityId, String title,
                                           Instant startAt, Instant endAt, AppointmentStatus status,
                                           AppointmentSource source, LocationKind locationKind,
                                           String locationDetail, String meetingUrl, String notes,
                                           String cancelReason, String publicToken, String googleEventId,
                                           UUID createdBy, Instant createdAt, Instant updatedAt) {
        return new Appointment(id, companyId, appointmentTypeId, hostId, contactId, opportunityId,
                title, startAt, endAt, status, source, locationKind, locationDetail, meetingUrl,
                notes, cancelReason, publicToken, googleEventId, createdBy, createdAt, updatedAt);
    }

    public void update(String title, UUID contactId, UUID opportunityId, LocationKind locationKind,
                       String locationDetail, String notes) {
        if (status == AppointmentStatus.CANCELED) {
            throw new SchedulingValidationException("Agendamento cancelado não pode ser alterado.");
        }
        if (title != null && !title.isBlank()) {
            validateTitle(title);
            this.title = title.trim();
        }
        this.contactId = contactId;
        this.opportunityId = opportunityId;
        this.locationKind = locationKind;
        this.locationDetail = locationDetail;
        this.notes = notes;
        touch();
    }

    public void reschedule(Instant newStart, Instant newEnd) {
        if (status == AppointmentStatus.CANCELED) {
            throw new SchedulingValidationException("Agendamento cancelado não pode ser remarcado.");
        }
        validateRange(newStart, newEnd);
        this.startAt = newStart;
        this.endAt = newEnd;
        this.status = AppointmentStatus.SCHEDULED;
        touch();
    }

    public void confirm() {
        if (status != AppointmentStatus.SCHEDULED) {
            throw new SchedulingValidationException("Apenas agendamentos pendentes podem ser confirmados.");
        }
        this.status = AppointmentStatus.CONFIRMED;
        touch();
    }

    public void cancel(String reason) {
        if (status == AppointmentStatus.CANCELED) {
            return;
        }
        this.status = AppointmentStatus.CANCELED;
        this.cancelReason = reason;
        touch();
    }

    public void complete() {
        if (status == AppointmentStatus.CANCELED) {
            throw new SchedulingValidationException("Agendamento cancelado não pode ser concluído.");
        }
        this.status = AppointmentStatus.COMPLETED;
        touch();
    }

    public void markNoShow() {
        if (status == AppointmentStatus.CANCELED) {
            throw new SchedulingValidationException("Agendamento cancelado não pode ser marcado como falta.");
        }
        this.status = AppointmentStatus.NO_SHOW;
        touch();
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new SchedulingValidationException("O título do agendamento é obrigatório.");
        }
        if (title.trim().length() > 200) {
            throw new SchedulingValidationException("O título deve ter no máximo 200 caracteres.");
        }
    }

    private static void validateRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new SchedulingValidationException("Data de início e fim são obrigatórias.");
        }
        if (!end.isAfter(start)) {
            throw new SchedulingValidationException("A data de fim deve ser posterior à de início.");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getAppointmentTypeId() { return appointmentTypeId; }
    public UUID getHostId() { return hostId; }
    public UUID getContactId() { return contactId; }
    public UUID getOpportunityId() { return opportunityId; }
    public String getTitle() { return title; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public AppointmentStatus getStatus() { return status; }
    public AppointmentSource getSource() { return source; }
    public LocationKind getLocationKind() { return locationKind; }
    public String getLocationDetail() { return locationDetail; }
    public String getMeetingUrl() { return meetingUrl; }
    public String getNotes() { return notes; }
    public String getCancelReason() { return cancelReason; }
    public String getPublicToken() { return publicToken; }
    public String getGoogleEventId() { return googleEventId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
