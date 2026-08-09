package com.becommerce.crm.infrastructure.company.persistence;

import com.becommerce.crm.domain.company.CompanySettings;
import org.springframework.stereotype.Component;

@Component
public class CompanySettingsMapper {

    public CompanySettingsJpaEntity toJpaEntity(CompanySettings settings) {
        CompanySettingsJpaEntity entity = new CompanySettingsJpaEntity();
        entity.setId(settings.getId());
        entity.setCompanyId(settings.getCompanyId());
        entity.setTimezone(settings.getTimezone());
        entity.setLocale(settings.getLocale());
        entity.setCurrency(settings.getCurrency());
        entity.setBusinessHours(settings.getBusinessHours());
        entity.setNotificationPreferences(settings.getNotificationPreferences());
        entity.setCreatedAt(settings.getCreatedAt());
        entity.setUpdatedAt(settings.getUpdatedAt());
        return entity;
    }

    public CompanySettings toDomainEntity(CompanySettingsJpaEntity entity) {
        return CompanySettings.reconstitute(
                entity.getId(),
                entity.getCompanyId(),
                entity.getTimezone(),
                entity.getLocale(),
                entity.getCurrency(),
                entity.getBusinessHours(),
                entity.getNotificationPreferences(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
