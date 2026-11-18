package com.becommerce.crm.finance.cardfee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardFeePresetJpaRepository extends JpaRepository<CardFeePresetJpaEntity, UUID> {

    List<CardFeePresetJpaEntity> findByActiveTrueOrderByNameAsc();

    List<CardFeePresetJpaEntity> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
