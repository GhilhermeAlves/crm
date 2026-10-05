package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.sales.scheduling.application.dto.BirthdayMessageSettingsDto;
import com.becommerce.crm.sales.scheduling.application.port.in.BirthdayMessageSettingsUseCase;
import com.becommerce.crm.sales.scheduling.infrastructure.persistence.BirthdayMessageSettingsJpaEntity;
import com.becommerce.crm.sales.scheduling.infrastructure.persistence.BirthdayMessageSettingsJpaRepository;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BirthdayMessageSettingsService implements BirthdayMessageSettingsUseCase {

    private final BirthdayMessageSettingsJpaRepository repository;

    public BirthdayMessageSettingsService(BirthdayMessageSettingsJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BirthdayMessageSettingsDto> get(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findById(companyId)
                    .map(e -> new BirthdayMessageSettingsDto(e.isEnabled(), e.getTemplate()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public BirthdayMessageSettingsDto save(UUID companyId, BirthdayMessageSettingsDto request) {
        try {
            TenantContext.setCompanyId(companyId);
            BirthdayMessageSettingsJpaEntity entity = repository.findById(companyId)
                    .orElseGet(BirthdayMessageSettingsJpaEntity::new);
            entity.setCompanyId(companyId);
            entity.setEnabled(request.enabled());
            entity.setTemplate(request.template());
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
            return new BirthdayMessageSettingsDto(entity.isEnabled(), entity.getTemplate());
        } finally {
            TenantContext.clear();
        }
    }
}
