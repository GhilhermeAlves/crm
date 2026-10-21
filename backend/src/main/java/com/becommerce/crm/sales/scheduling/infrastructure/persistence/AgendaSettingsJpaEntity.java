package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "agenda_settings")
public class AgendaSettingsJpaEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "default_duration_minutes")
    private int defaultDurationMinutes;

    @Column(name = "work_hours_start")
    private LocalTime workHoursStart;

    @Column(name = "work_hours_end")
    private LocalTime workHoursEnd;

    @Column(name = "notify_on_booking")
    private boolean notifyOnBooking;

    @Column(name = "notify_24h_before")
    private boolean notify24hBefore;

    @Column(name = "notify_1h_before")
    private boolean notify1hBefore;

    @Column(name = "notify_on_cancel")
    private boolean notifyOnCancel;

    @Column(name = "notify_on_reschedule")
    private boolean notifyOnReschedule;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(int v) { this.defaultDurationMinutes = v; }
    public LocalTime getWorkHoursStart() { return workHoursStart; }
    public void setWorkHoursStart(LocalTime v) { this.workHoursStart = v; }
    public LocalTime getWorkHoursEnd() { return workHoursEnd; }
    public void setWorkHoursEnd(LocalTime v) { this.workHoursEnd = v; }
    public boolean isNotifyOnBooking() { return notifyOnBooking; }
    public void setNotifyOnBooking(boolean v) { this.notifyOnBooking = v; }
    public boolean isNotify24hBefore() { return notify24hBefore; }
    public void setNotify24hBefore(boolean v) { this.notify24hBefore = v; }
    public boolean isNotify1hBefore() { return notify1hBefore; }
    public void setNotify1hBefore(boolean v) { this.notify1hBefore = v; }
    public boolean isNotifyOnCancel() { return notifyOnCancel; }
    public void setNotifyOnCancel(boolean v) { this.notifyOnCancel = v; }
    public boolean isNotifyOnReschedule() { return notifyOnReschedule; }
    public void setNotifyOnReschedule(boolean v) { this.notifyOnReschedule = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
