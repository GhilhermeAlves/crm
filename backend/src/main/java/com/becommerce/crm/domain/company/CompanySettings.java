package com.becommerce.crm.domain.company;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class CompanySettings {

    public static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";
    public static final String DEFAULT_LOCALE = "pt-BR";
    public static final String DEFAULT_CURRENCY = "BRL";

    private final UUID id;
    private final UUID companyId;
    private String timezone;
    private String locale;
    private String currency;
    private String businessHours;
    private String notificationPreferences;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private CompanySettings(
            UUID id,
            UUID companyId,
            String timezone,
            String locale,
            String currency,
            String businessHours,
            String notificationPreferences,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.timezone = timezone;
        this.locale = locale;
        this.currency = currency;
        this.businessHours = businessHours;
        this.notificationPreferences = notificationPreferences;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CompanySettings create(
            UUID companyId,
            String timezone,
            String locale,
            String currency,
            String businessHours,
            String notificationPreferences
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new CompanySettings(
                UUID.randomUUID(),
                companyId,
                timezone,
                locale,
                currency,
                businessHours,
                notificationPreferences,
                now,
                now
        );
    }

    public static CompanySettings reconstitute(
            UUID id,
            UUID companyId,
            String timezone,
            String locale,
            String currency,
            String businessHours,
            String notificationPreferences,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new CompanySettings(
                id,
                companyId,
                timezone,
                locale,
                currency,
                businessHours,
                notificationPreferences,
                createdAt,
                updatedAt
        );
    }

    public void update(
            String timezone,
            String locale,
            String currency,
            String businessHours,
            String notificationPreferences
    ) {
        this.timezone = timezone;
        this.locale = locale;
        this.currency = currency;
        this.businessHours = businessHours;
        this.notificationPreferences = notificationPreferences;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getTimezone() { return timezone; }
    public String getLocale() { return locale; }
    public String getCurrency() { return currency; }
    public String getBusinessHours() { return businessHours; }
    public String getNotificationPreferences() { return notificationPreferences; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanySettings that = (CompanySettings) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
