package com.becommerce.crm.infrastructure.company.persistence;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.domain.company.Company;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CompanyRepositoryImpl implements CompanyRepository {

    private final SpringDataCompanyRepository repository;
    private final CompanyMapper mapper;

    public CompanyRepositoryImpl(SpringDataCompanyRepository repository, CompanyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Company save(Company company) {
        CompanyJpaEntity entity = mapper.toJpaEntity(company);
        if (company.getId() == null || !repository.existsById(company.getId())) {
            entity.setId(null);
        }
        CompanyJpaEntity saved = repository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<Company> findByCnpj(String cnpj) {
        return repository.findByCnpj(cnpj).map(mapper::toDomainEntity);
    }

    @Override
    public boolean existsByCnpj(String cnpj) {
        return repository.existsByCnpj(cnpj);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public List<Company> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
