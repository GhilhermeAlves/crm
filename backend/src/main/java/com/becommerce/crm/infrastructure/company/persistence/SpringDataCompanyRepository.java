package com.becommerce.crm.infrastructure.company.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCompanyRepository extends JpaRepository<CompanyJpaEntity, UUID> {
    Optional<CompanyJpaEntity> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    boolean existsByEmail(String email);
}
