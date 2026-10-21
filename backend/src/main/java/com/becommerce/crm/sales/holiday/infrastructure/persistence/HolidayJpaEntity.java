package com.becommerce.crm.sales.holiday.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_holidays")
public class HolidayJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    private LocalDate date;

    private String name;

    @Column(name = "recurring_yearly")
    private boolean recurringYearly;

    @Column(name = "blocks_schedule")
    private boolean blocksSchedule;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRecurringYearly() {
        return recurringYearly;
    }

    public void setRecurringYearly(boolean recurringYearly) {
        this.recurringYearly = recurringYearly;
    }

    public boolean isBlocksSchedule() {
        return blocksSchedule;
    }

    public void setBlocksSchedule(boolean blocksSchedule) {
        this.blocksSchedule = blocksSchedule;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
