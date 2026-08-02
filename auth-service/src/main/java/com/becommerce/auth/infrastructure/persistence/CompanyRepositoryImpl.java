package com.becommerce.auth.infrastructure.persistence;

import com.becommerce.auth.application.company.port.output.CompanyRepository;
import com.becommerce.auth.domain.company.CompanyStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do {@link CompanyRepository} via Spring Data (somente leitura).
 */
@Repository
public class CompanyRepositoryImpl implements CompanyRepository {

    private final SpringDataCompanyRepository springData;

    public CompanyRepositoryImpl(SpringDataCompanyRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<CompanyStatus> findStatusById(UUID companyId) {
        return springData.findById(companyId).map(CompanyJpaEntity::getStatus)
                .map(status -> {
                    try {
                        return CompanyStatus.valueOf(status);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });
    }
}
