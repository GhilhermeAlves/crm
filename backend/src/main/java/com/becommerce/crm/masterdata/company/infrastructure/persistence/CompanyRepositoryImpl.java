package com.becommerce.crm.masterdata.company.infrastructure.persistence;

import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
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
    public Optional<UUID> findIdByCnpj(String cnpj) {
        return cnpj == null ? Optional.empty() : Optional.ofNullable(repository.findIdByCnpj(cnpj));
    }

    @Override
    public boolean existsByCnpj(String cnpj) {
        return findIdByCnpj(cnpj).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && repository.existsByEmail(email);
    }

    @Override
    public List<Company> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<UUID> findAllIds() {
        return repository.findAllIds();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
