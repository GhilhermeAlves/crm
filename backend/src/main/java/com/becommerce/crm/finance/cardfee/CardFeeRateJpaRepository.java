package com.becommerce.crm.finance.cardfee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CardFeeRateJpaRepository extends JpaRepository<CardFeeRateJpaEntity, UUID> {

    List<CardFeeRateJpaEntity> findByCompanyIdOrderByInstallmentsAsc(UUID companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CardFeeRateJpaEntity r where r.companyId = :companyId")
    void deleteAllOfCompany(@Param("companyId") UUID companyId);
}
