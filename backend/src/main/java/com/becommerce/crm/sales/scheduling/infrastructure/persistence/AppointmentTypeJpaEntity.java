package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment_types")
public class AppointmentTypeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    private String name;
    private String slug;
    private String description;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "buffer_before_minutes")
    private int bufferBeforeMinutes;

    @Column(name = "buffer_after_minutes")
    private int bufferAfterMinutes;

    @Column(name = "min_notice_hours")
    private int minNoticeHours;

    @Column(name = "max_days_ahead")
    private int maxDaysAhead;

    @Column(name = "slot_interval_minutes")
    private int slotIntervalMinutes;

    private String color;

    @Column(name = "location_kind")
    private String locationKind;

    @Column(name = "location_detail")
    private String locationDetail;

    @Column(name = "assignment_mode")
    private String assignmentMode;

    @Column(name = "public_booking_enabled")
    private boolean publicBookingEnabled;

    private boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getBufferBeforeMinutes() { return bufferBeforeMinutes; }
    public void setBufferBeforeMinutes(int bufferBeforeMinutes) { this.bufferBeforeMinutes = bufferBeforeMinutes; }
    public int getBufferAfterMinutes() { return bufferAfterMinutes; }
    public void setBufferAfterMinutes(int bufferAfterMinutes) { this.bufferAfterMinutes = bufferAfterMinutes; }
    public int getMinNoticeHours() { return minNoticeHours; }
    public void setMinNoticeHours(int minNoticeHours) { this.minNoticeHours = minNoticeHours; }
    public int getMaxDaysAhead() { return maxDaysAhead; }
    public void setMaxDaysAhead(int maxDaysAhead) { this.maxDaysAhead = maxDaysAhead; }
    public int getSlotIntervalMinutes() { return slotIntervalMinutes; }
    public void setSlotIntervalMinutes(int slotIntervalMinutes) { this.slotIntervalMinutes = slotIntervalMinutes; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getLocationKind() { return locationKind; }
    public void setLocationKind(String locationKind) { this.locationKind = locationKind; }
    public String getLocationDetail() { return locationDetail; }
    public void setLocationDetail(String locationDetail) { this.locationDetail = locationDetail; }
    public String getAssignmentMode() { return assignmentMode; }
    public void setAssignmentMode(String assignmentMode) { this.assignmentMode = assignmentMode; }
    public boolean isPublicBookingEnabled() { return publicBookingEnabled; }
    public void setPublicBookingEnabled(boolean publicBookingEnabled) { this.publicBookingEnabled = publicBookingEnabled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
