package com.becommerce.crm.infrastructure.company.persistence;

import com.becommerce.crm.application.company.port.output.CompanySettingsRepository;
import com.becommerce.crm.domain.company.CompanySettings;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CompanySettingsRepositoryImpl implements CompanySettingsRepository {

    private final SpringDataCompanySettingsRepository repository;
    private final CompanySettingsMapper mapper;

    public CompanySettingsRepositoryImpl(SpringDataCompanySettingsRepository repository,
                                         CompanySettingsMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CompanySettings> findByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId).map(mapper::toDomainEntity);
    }

    @Override
    public CompanySettings save(CompanySettings settings) {
        CompanySettingsJpaEntity entity = mapper.toJpaEntity(settings);
        boolean exists = repository.existsById(entity.getId());
        entity.setNewRecord(!exists);
        if (!exists) {
            entity.setId(null);
        }
        CompanySettingsJpaEntity saved = repository.save(entity);
        return mapper.toDomainEntity(saved);
    }
}
