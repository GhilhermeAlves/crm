package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "birthday_message_settings")
public class BirthdayMessageSettingsJpaEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    private boolean enabled;

    private String template;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
