package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class AppointmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "appointment_type_id")
    private UUID appointmentTypeId;

    @Column(name = "host_id")
    private UUID hostId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    private String title;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    private String status;
    private String source;

    @Column(name = "location_kind")
    private String locationKind;

    @Column(name = "location_detail")
    private String locationDetail;

    @Column(name = "meeting_url")
    private String meetingUrl;

    private String notes;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "public_token")
    private String publicToken;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getAppointmentTypeId() { return appointmentTypeId; }
    public void setAppointmentTypeId(UUID appointmentTypeId) { this.appointmentTypeId = appointmentTypeId; }
    public UUID getHostId() { return hostId; }
    public void setHostId(UUID hostId) { this.hostId = hostId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public UUID getOpportunityId() { return opportunityId; }
    public void setOpportunityId(UUID opportunityId) { this.opportunityId = opportunityId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLocationKind() { return locationKind; }
    public void setLocationKind(String locationKind) { this.locationKind = locationKind; }
    public String getLocationDetail() { return locationDetail; }
    public void setLocationDetail(String locationDetail) { this.locationDetail = locationDetail; }
    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
