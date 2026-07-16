package com.becommerce.crm.application.company.port.output;

import com.becommerce.crm.domain.company.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(UUID id);
    Optional<Company> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    boolean existsByEmail(String email);
    List<Company> findAll();
    void deleteById(UUID id);
}
