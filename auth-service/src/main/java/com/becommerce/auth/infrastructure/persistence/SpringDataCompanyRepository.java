package com.becommerce.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositório Spring Data de leitura sobre {@code companies} (somente status).
 */
public interface SpringDataCompanyRepository extends JpaRepository<CompanyJpaEntity, UUID> {
}
