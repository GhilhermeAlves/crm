package com.becommerce.crm.masterdata.company.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCompanyRepository extends JpaRepository<CompanyJpaEntity, UUID> {
    // V134: companies tem RLS; estas consultas precisam de todas as empresas.
    @Query(value = "SELECT app.company_id_by_cnpj(:cnpj)", nativeQuery = true)
    UUID findIdByCnpj(@Param("cnpj") String cnpj);

    @Query(value = "SELECT app.company_email_exists(:email)", nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);

    @Query(value = "SELECT id FROM app.all_company_ids()", nativeQuery = true)
    List<UUID> findAllIds();
}
